package uk.gov.nationalarchives.externalevent.events

import uk.gov.nationalarchives.externalevent.decoders.DR2EventDecoder.DR2Event
import com.typesafe.config.{Config, ConfigFactory}
import uk.gov.nationalarchives.aws.utils.s3.{S3Clients, S3Utils}
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.logging.LogLevel

import scala.util.Try

object DR2EventHandler {

  private val configFactory: Config = ConfigFactory.load()
  private val bucket = configFactory.getString("s3.standardBucket")
  private val s3Utils = S3Utils(S3Clients.s3Async(configFactory.getString("s3.endpoint")))

  def handleEvent(ev: DR2Event)(implicit logger: LambdaLogger): Unit = {
    val tags = ev.messageType match {
      case "preserve.digital.asset.ingest.complete" => Map("PreserveDigitalAssetIngest" -> "Complete")
      case _ => Map("UnknownDR2Message" -> s"${ev.messageType}")
    }

    List(ev.assetId,s"${ev.assetId}.metadata" ).foreach { key =>
      Try(
      s3Utils.addObjectTags(bucket, key, tags).attempt.unsafeRunSync() match {
        case Left(err) => logger.log(s"Error adding tags to $key: ${err.getMessage}", LogLevel.ERROR)
        case Right(_) => logger.log(s"Tags added successfully to $key", LogLevel.INFO)
      }).recover {
        case e: Exception =>
          logger.log(s"An error occurred while adding tags to $key: ${e.getMessage}", LogLevel.ERROR)
      }
    }
  }

}
