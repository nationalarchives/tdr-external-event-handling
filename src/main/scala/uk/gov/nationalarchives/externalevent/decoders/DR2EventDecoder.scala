package uk.gov.nationalarchives.externalevent.decoders

import io.circe.{Decoder, HCursor, Json}

object DR2EventDecoder {
  case class DR2Event(properties: Json, parameters: Json, timestamp: String, topicArn: String, messageType: String, assetId: String) extends IncomingEvent

  implicit val decodeDR2Event: Decoder[DR2Event] = new Decoder[DR2Event] {
    final def apply(c: HCursor): Decoder.Result[DR2Event] = {
      for {
        props <- c.downField("body").downField("properties").as[Json]
        params <- c.downField("body").downField("parameters").as[Json]
        time <- c.downField("timestamp").as[String]
        topic <- c.downField("topicArn").as[String]
        messageType <- c.downField("body").downField("properties").downField("messageType").as[String]
        assetId <- c.downField("body").downField("parameters").downField("assetId").as[String]
      } yield {
        DR2Event(props, params, time, topic, messageType, assetId)
      }
    }
  }

}
