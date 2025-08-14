package uk.gov.nationalarchives.externalevent.events

import uk.gov.nationalarchives.externalevent.decoders.DR2EventDecoder.DR2Event
import com.typesafe.config.{Config, ConfigFactory}
import uk.gov.nationalarchives.aws.utils.s3.{S3Clients, S3Utils}
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import uk.gov.nationalarchives.externalevent.GraphQlApi
import graphql.codegen.AddMultipleFileStatuses.{addMultipleFileStatuses => amfs}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object DR2EventHandler {

  private val config: Config = ConfigFactory.load()
  private val bucket = config.getString("s3.standardBucket")
  private val s3Utils = S3Utils(S3Clients.s3Async(config.getString("s3.endpoint")))
  private val ingestKey = config.getString("tags.dr2IngestKey")
  private val ingestValue = config.getString("tags.dr2IngestValue")

  def handleEvent(ev: DR2Event, doUpdate: Boolean = config.getBoolean("allowFileStatusUpdate"))(implicit logger: LambdaLogger): Unit = {
    val tags = ev.messageType match {
      case "preserve.digital.asset.ingest.complete" => Map(ingestKey -> ingestValue)
      case _                                        => Map("UnknownDR2Message" -> s"${ev.messageType}")
    }

    List(ev.assetId, s"${ev.assetId}.metadata").foreach { key =>
      Try(s3Utils.addObjectTags(bucket, key, tags).attempt.unsafeRunSync() match {
        case Left(err) => logger.log(s"Error adding tags to $key: ${err.getMessage}", LogLevel.ERROR)
        case Right(_)  => logger.log(s"Tags added successfully to $key", LogLevel.INFO)
          if(key == ev.assetId) {updateFileStatus(ev.assetId, tags.head._1, tags.head._2, doUpdate)}
      }).recover { case e: Exception =>
        logger.log(s"An error occurred while adding tags to $key: ${e.getMessage}", LogLevel.ERROR)
      }
    }
  }

  private def updateFileStatus(assetId: String, statusType: String, statusValue: String, doUpdate: Boolean)(implicit logger: LambdaLogger): amfs.Data = {
    if(doUpdate) {
      implicit val ec: ExecutionContext = ExecutionContext.global
      logger.log(s"Updating file status for asset $assetId with type $statusType and value $statusValue", LogLevel.INFO)
      val graphQlApi: GraphQlApi = GraphQlApi(config)(logger)
      val resp = graphQlApi.updateFileStatus(assetId, statusType, statusValue)
      resp
    } else amfs.Data(List.empty)
  }
}
