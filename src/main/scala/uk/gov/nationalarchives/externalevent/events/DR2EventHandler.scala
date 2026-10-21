package uk.gov.nationalarchives.externalevent.events

import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import com.typesafe.config.{Config, ConfigFactory}
import graphql.codegen.AddMultipleFileStatuses.{addMultipleFileStatuses => amfs}
import software.amazon.awssdk.services.s3.model.S3Exception
import uk.gov.nationalarchives.aws.utils.s3.{S3Clients, S3Utils}
import uk.gov.nationalarchives.externalevent.GraphQlApi
import uk.gov.nationalarchives.externalevent.decoders.DR2EventDecoder.DR2Event
import uk.gov.nationalarchives.tdr.common.utils.objectkeycontext.Context
import uk.gov.nationalarchives.tdr.common.utils.objectkeycontext.ObjectTypes.Record

import java.util.concurrent.{CompletionException, ExecutionException}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object DR2EventHandler {

  private val config: Config = ConfigFactory.load()
  private val bucket = config.getString("s3.standardBucket")
  private val s3Utils = S3Utils(S3Clients.s3Async(config.getString("s3.endpoint")))
  private val ingestKey = config.getString("tags.dr2IngestKey")
  private val ingestValue = config.getString("tags.dr2IngestValue")
  private val consignmentIdTag = config.getString("tags.consignmentId")
  private val userIdTag = config.getString("tags.userId")
  private val assetSourceTag = config.getString("tags.assetSource")

  def handleEvent(ev: DR2Event, doUpdate: Boolean = config.getBoolean("allowFileStatusUpdate"))(implicit logger: LambdaLogger): Unit = {
    val tags = ev.messageType match {
      case "preserve.digital.asset.ingest.complete" => Map(ingestKey -> ingestValue)
      case _                                        => Map("UnknownDR2Message" -> s"${ev.messageType}")
    }

    s3Utils.listAllObjectsWithPrefix(bucket, ev.assetId).map(_.key).foreach { key =>
      val objectContext = Context.objectKeyParser(key, bucket)
      Try(s3Utils.addObjectTags(bucket, key, tags).attempt.unsafeRunSync() match {
        case Left(err) => logger.log(s"Error adding tags to $key: ${err.getMessage}", LogLevel.ERROR)
        case Right(_)  =>
          logger.log(s"Tags added successfully to $key", LogLevel.INFO)
          if (objectContext.objectType.contains(Record)) {
            val fileId = objectContext.fileId.getOrElse(throw new IllegalArgumentException(s"Missing file ID in export record key $key")).toString
            if (ev.messageType == "preserve.digital.asset.ingest.complete") tagUpstreamObjects(key, fileId, tags)
            updateFileStatus(fileId, tags.head._1, tags.head._2, doUpdate)
          }
      }).recover { case e: Exception =>
        logger.log(s"An error occurred while adding tags to $key: ${e.getMessage}", LogLevel.ERROR)
      }
    }
  }

  private def tagUpstreamObjects(exportKey: String, fileId: String, tags: Map[String, String])(implicit logger: LambdaLogger): Unit = {
    Try {
      val contextTags = s3Utils.getObjectTags(bucket, exportKey)
      val consignmentId = contextTags.get(consignmentIdTag).filter(_.nonEmpty)
      val userId = contextTags.get(userIdTag).filter(_.nonEmpty)
      (consignmentId, userId) match {
        case (Some(consignment), Some(user)) =>
          val upstreamKey = s"$consignment/$fileId"
          val dirtyKey = (List(user) ++ contextTags.get(assetSourceTag).filter(_.nonEmpty) ++ List(consignment, fileId)).mkString("/")
          List(
            config.getString("s3.dirtyBucket") -> dirtyKey,
            config.getString("s3.cleanBucket") -> upstreamKey,
            config.getString("s3.quarantineBucket") -> upstreamKey
          ).foreach { case (upstreamBucket, key) =>
            tagUpstreamObject(upstreamBucket, key, tags)
          }
        case _ =>
          logger.log(s"Skipping upstream tagging for $exportKey: missing $consignmentIdTag or $userIdTag tag", LogLevel.WARN)
      }
    }.recover { case e: Exception =>
      logger.log(s"Error reading contextual tags for $exportKey: ${e.getMessage}", LogLevel.ERROR)
    }
  }

  private def tagUpstreamObject(upstreamBucket: String, key: String, tags: Map[String, String])(implicit logger: LambdaLogger): Unit = {
    Try(s3Utils.addObjectTags(upstreamBucket, key, tags).unsafeRunSync()) match {
      case Success(_) =>
        logger.log(s"Tags added successfully to s3://$upstreamBucket/$key", LogLevel.INFO)
      case Failure(error) =>
        underlyingError(error) match {
          case e: S3Exception if e.statusCode() == 404 && Option(e.awsErrorDetails()).exists(_.errorCode() == "NoSuchKey") =>
            logger.log(s"Skipping missing upstream object s3://$upstreamBucket/$key", LogLevel.WARN)
          case e =>
            logger.log(s"Error adding tags to s3://$upstreamBucket/$key: ${e.getMessage}", LogLevel.ERROR)
        }
    }
  }

  @tailrec
  private def underlyingError(error: Throwable): Throwable = error match {
    case e: ExecutionException if e.getCause != null  => underlyingError(e.getCause)
    case e: CompletionException if e.getCause != null => underlyingError(e.getCause)
    case e                                           => e
  }

  private def updateFileStatus(assetId: String, statusType: String, statusValue: String, doUpdate: Boolean)(implicit logger: LambdaLogger): amfs.Data = {
    if (doUpdate) {
      implicit val ec: ExecutionContext = ExecutionContext.global
      logger.log(s"Updating file status for asset $assetId with type $statusType and value $statusValue", LogLevel.INFO)
      val graphQlApi: GraphQlApi = GraphQlApi(config)(logger)
      val resp = graphQlApi.updateFileStatus(assetId, statusType, statusValue)
      resp
    } else amfs.Data(List.empty)
  }
}
