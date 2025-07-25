package uk.gov.nationalarchives.externalevent

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.parser.decode
import uk.gov.nationalarchives.externalevent.decoders._
import TestUtils._

class EventDecoderSpec extends AnyFlatSpec with Matchers {

  "A generic event" should "be decoded from a message" in {
    val decoded = decode[IncomingEvent](sqsMessage(genericMessage).getBody).toOption
    decoded should not be empty
    decoded.get shouldBe a[GenericEventDecoder.GenericEvent]
  }

  "A DR2 Event" should "be decoded from a standard message" in {
    val decoded = decode[IncomingEvent](sqsMessage(standardDR2Message).getBody).toOption
    decoded should not be empty
    decoded.get shouldBe a[DR2EventDecoder.DR2Event]
  }

  "A DR2 Event" should "be decoded when message has extra content" in {
    val decoded = decode[IncomingEvent](sqsMessage(nonStandardDR2Message).getBody).toOption
    decoded should not be empty
    decoded.get shouldBe a[DR2EventDecoder.DR2Event]
  }

  "A DR2 Event" should "be decoded when message type has unrecognised content " in {
    val decoded = decode[IncomingEvent](sqsMessage(incorrectDR2MessageType).getBody).toOption
    decoded should not be empty
    decoded.get shouldBe a[DR2EventDecoder.DR2Event]
  }

  "A DR2 Event" should "not be decoded when message content has incorrect keys" in {
    val decoded = decode[IncomingEvent](sqsMessage(incorrectDR2Message1).getBody).toOption
    decoded should not be empty
    decoded.get should not be a[DR2EventDecoder.DR2Event]
  }

  "A DR2 Event" should "not be decoded when message content has missing values" in {
    val decoded = decode[IncomingEvent](sqsMessage(incorrectDR2Message2).getBody).toOption
    decoded shouldBe empty
  }

  "A non JSON message" should "return None" in {
    val decoded = decode[IncomingEvent](sqsMessage(notJSON).getBody).toOption
    decoded shouldBe None
  }
}
