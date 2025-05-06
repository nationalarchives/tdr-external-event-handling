package uk.gov.nationalarchives.externalevent.decoders

import io.circe.{Decoder, HCursor, Json}

object GenericEventDecoder {
  case class GenericEvent(content: Json) extends IncomingEvent

  implicit val decodeGenericEvent: Decoder[GenericEvent] = (c: HCursor) => {
    for {
      content <- c.value.as[Json]
    } yield {
      GenericEvent(content)
    }
  }
}
