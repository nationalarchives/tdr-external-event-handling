package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import io.circe.parser.decode
//import uk.gov.nationalarchives.externalevent.decoders.IncomingEvent
import uk.gov.nationalarchives.externalevent.decoders._
import uk.gov.nationalarchives.externalevent.events._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class Lambda extends RequestHandler[SQSEvent, String] {

  override def handleRequest(event: SQSEvent, context: Context): String = {
    val sqsMessages: Seq[SQSMessage] = event.getRecords.asScala.toList
    sqsMessages.map(message => {
      //TODO Validate event source
      decode[IncomingEvent](message.getBody).map {

        case dr2Event: DR2EventDecoder.DR2Event => DR2EventHandler.handleEvent(dr2Event)
        case _                                  => println("Unrecognised event type") //TODO Throw unrecognised event error
      }
    })
    "Lambda has run"
  }

  def handleEvent[T <: IncomingEvent](incomingEvent: T): Unit = {
    //println(s"Handling event: $incomingEvent")
    incomingEvent match {
      case ev: DR2EventDecoder.DR2Event => println("Generic handleEvent")

    }
  }

}
