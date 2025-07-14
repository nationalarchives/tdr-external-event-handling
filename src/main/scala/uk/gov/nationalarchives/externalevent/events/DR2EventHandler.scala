package uk.gov.nationalarchives.externalevent.events

import cats.effect.IO
import uk.gov.nationalarchives.externalevent.decoders.DR2EventDecoder.DR2Event
import com.typesafe.config.{Config, ConfigFactory}
import uk.gov.nationalarchives.aws.utils.s3.{S3Clients, S3Utils}
import cats.effect.unsafe.implicits.global
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object DR2EventHandler {

  private val configFactory: Config = ConfigFactory.load()
  private val bucket = configFactory.getString("s3.standardBucket")
  private val s3Utils = S3Utils(S3Clients.s3Async(configFactory.getString("s3.endpoint")))
  private implicit def logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def handleEvent(ev: DR2Event): Unit = {

    val tags = ev.messageType match {
      case "preserve.digital.asset.ingest.complete" => Map("PreserveDigitalAssetIngest" -> "Complete")
      case _ => Map("UnknownDR2Message" -> s"${ev.messageType}")
    }

    //TODO Error handling - S3Exception, FileNotFoundException, etc.
    /*
    software.amazon.awssdk.services.s3.model.NoSuchKeyException: The specified key does not exist. - No file

     */

    List(ev.assetId,s"${ev.assetId}.metadata" ).foreach { key =>
      s3Utils.addObjectTags(bucket, key, tags).attempt.unsafeRunSync() match {
        case Left(err) => println(s"Error adding tags to $key: ${err.getMessage}")
        case Right(_) => println(s"Tags added successfully to $key")
      }
    }

  }

}
