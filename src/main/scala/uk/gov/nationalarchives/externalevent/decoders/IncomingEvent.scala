package uk.gov.nationalarchives.externalevent.decoders

import uk.gov.nationalarchives.externalevent.decoders.DR2EventDecoder.DR2Event
import uk.gov.nationalarchives.externalevent.decoders.GenericEventDecoder.GenericEvent
import io.circe.{Decoder, HCursor, Json}

trait IncomingEvent {}

object IncomingEvent {
  implicit val allDecoders: Decoder[IncomingEvent] = decodeSQSEvent[DR2Event] or decodeSQSEvent[GenericEvent]

  def decodeSQSEvent[T <: IncomingEvent]()(implicit decoder: Decoder[T]): Decoder[IncomingEvent] = (c: HCursor) => {
    for {
      message <- c.value.as[Json]
      event <- message.as[T]
    } yield {
      event
    }
  }
}
