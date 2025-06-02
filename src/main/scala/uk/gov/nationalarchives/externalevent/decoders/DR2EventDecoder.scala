package uk.gov.nationalarchives.externalevent.decoders

import io.circe.{Decoder, HCursor, Json}

object DR2EventDecoder {
  case class DR2Event(properties: Json, parameters: Json, timestamp: String, topicArn: String) extends IncomingEvent

  implicit val decodeDR2Event: Decoder[DR2Event] = new Decoder[DR2Event] {
    final def apply(c: HCursor): Decoder.Result[DR2Event] = {
      for {
        props <- c.downField("properties").as[Json]
        params <- c.downField("parameters").as[Json]
        time <- c.downField("timestamp").as[String]
        topic <- c.downField("topicArn").as[String]

      } yield {
        DR2Event(props, params, time, topic)
      }
    }
  }

}
