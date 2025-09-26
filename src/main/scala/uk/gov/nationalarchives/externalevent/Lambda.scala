package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import io.circe.parser.decode
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import com.typesafe.config.ConfigFactory
import uk.gov.nationalarchives.externalevent.decoders._
import uk.gov.nationalarchives.externalevent.events._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class Lambda extends RequestHandler[SQSEvent, Unit] {

  override def handleRequest(event: SQSEvent, context: Context): Unit = {

    val debug: Boolean = ConfigFactory.load().getBoolean("debugIncomingMessage")
    val logger = context.getLogger

    logger.log("Calling External Events Handler", LogLevel.INFO)
    if (debug) logger.log(s"Debug ON", LogLevel.INFO)
    val sqsMessages: Seq[SQSMessage] = event.getRecords.asScala.toList
    sqsMessages.foreach(message => {
      if (debug) logger.log(s"Incoming message: ${message.getBody}", LogLevel.INFO)
      decode[IncomingEvent](message.getBody).foreach {
        case dr2Event: DR2EventDecoder.DR2Event => DR2EventHandler.handleEvent(dr2Event)(logger)
        case _                                  => logger.log("Unrecognised event type", LogLevel.WARN) // TODO Throw unrecognised event error
      }
    })
  }
}
