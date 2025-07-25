package uk.gov.nationalarchives.externalevent

import org.scalatest.matchers.should.Matchers
import io.circe.parser.decode
import uk.gov.nationalarchives.externalevent.decoders._
import uk.gov.nationalarchives.externalevent.events._
import com.github.tomakehurst.wiremock.client.WireMock._
import TestUtils._

class DR2IntegrationSpec extends ExternalServicesSpec with Matchers {

  "Lambda" should "correctly handle a DR2 message" in {
    mockS3GetResponse()
    mockS3PutResponse()

    new Lambda().handleRequest(
      getSQSEvent(List(standardDR2Message)),
      mockContext)

    wiremockS3.verify(putRequestedFor(anyUrl())
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "Lambda" should "correctly handle multiple DR2 messages" in {
    mockS3GetResponse()
    mockS3PutResponse()

    new Lambda().handleRequest(
      getSQSEvent(List(standardDR2Message, nonStandardDR2Message)),
      mockContext)

    wiremockS3.verify(4, putRequestedFor(anyUrl())
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "DR2EventHandler" should "pass correct tags when a standard DR2 Message is processed" in {
    val ev = runEventHandler(standardDR2Message)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "DR2EventHandler" should "apply tags to both file object and corresponding metadata object" in {
    val ev = runEventHandler(standardDR2Message)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}.metadata"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "DR2EventHandler" should "pass correct tags when a non-standard DR2 Message is processed" in {
    val ev = runEventHandler(nonStandardDR2Message)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "DR2EventHandler" should "pass alternative tags when a DR2 Message has an unrecognised message type" in {
    val ev = runEventHandler(incorrectDR2MessageType)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(unrecognisedPutTagsRequestXml(ev.messageType))))
  }

  def runEventHandler(eventType: String): DR2EventDecoder.DR2Event = {
    mockS3GetResponse()
    mockS3PutResponse()

    val ev = decode[DR2EventDecoder.DR2Event](sqsMessage(eventType).getBody)
      .getOrElse(throw new RuntimeException(s"Failed to decode DR2 Event of type: $eventType")) //TODO: Remove exception

    DR2EventHandler.handleEvent(ev)(mockContext.getLogger)
    ev
  }
}
