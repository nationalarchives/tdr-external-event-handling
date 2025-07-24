package uk.gov.nationalarchives.externalevent

import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import io.circe.parser.decode
import uk.gov.nationalarchives.externalevent.decoders._
import uk.gov.nationalarchives.externalevent.events._
import TestUtils._
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.api.client.logging.{LambdaContextLogger, StdOutLogSink}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.logging.{LogFormat, LogLevel}
import com.github.tomakehurst.wiremock.client.WireMock._
import scala.jdk.CollectionConverters.SeqHasAsJava

class DR2IntegrationSpec extends ExternalServicesSpec with Matchers {

  val expectedPutTagsRequestXml: String =
    """<?xml version="1.0" encoding="UTF-8"?><Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      |<TagSet><Tag><Key>PreserveDigitalAssetIngest</Key>
      |<Value>Complete</Value></Tag></TagSet></Tagging>""".stripMargin.replaceAll("\\n", "")

  def unrecognisedPutTagsRequestXml(tagValue: String) : String =
    s"""<?xml version="1.0" encoding="UTF-8"?><Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      |<TagSet><Tag><Key>UnknownDR2Message</Key>
      |<Value>$tagValue</Value></Tag></TagSet></Tagging>""".stripMargin.replaceAll("\\n", "")

  "Lambda" should "correctly handle a DR2 message" in {
    mockS3GetResponse()
    mockS3PutResponse()

    val message = new SQSEvent
    message.setRecords(List(sqsMessage(StandardDR2Message)).asJava)

    val mockContext = MockitoSugar.mock[Context]
    MockitoSugar.when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))

    new Lambda().handleRequest(message, mockContext)

    wiremockS3.verify(putRequestedFor(anyUrl())
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))

  }

  "Lambda" should "correctly handle multiple DR2 messages" in {
    mockS3GetResponse()
    mockS3PutResponse()

    val message = new SQSEvent
    message.setRecords(List(sqsMessage(StandardDR2Message), sqsMessage(NonStandardDR2Message)).asJava)

    val mockContext = MockitoSugar.mock[Context]
    MockitoSugar.when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))

    new Lambda().handleRequest(message, mockContext)

    wiremockS3.verify(4, putRequestedFor(anyUrl())
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))

  }

  "DR2EventHandler" should "pass correct tags when a standard DR2 Message is processed" in {
    val ev = runEventHandler(StandardDR2Message)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "DR2EventHandler" should "apply tags to both file object and corresponding metadata object" in {
    val ev = runEventHandler(StandardDR2Message)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}.metadata"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "DR2EventHandler" should "pass correct tags when a non-standard DR2 Message is processed" in {
    val ev = runEventHandler(NonStandardDR2Message)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(expectedPutTagsRequestXml)))
  }

  "DR2EventHandler" should "pass alternative tags when a DR2 Message has an unrecognised message type" in {
    val ev = runEventHandler(IncorrectDR2MessageType)

    wiremockS3.verify(putRequestedFor(urlPathEqualTo(s"/${ev.assetId}"))
      .withRequestBody(equalToXml(unrecognisedPutTagsRequestXml(ev.messageType))))
  }


  def runEventHandler(eventType: String): DR2EventDecoder.DR2Event = {
    mockS3GetResponse()
    mockS3PutResponse()

    val ev = decode[DR2EventDecoder.DR2Event](sqsMessage(eventType).getBody)
      .getOrElse(throw new RuntimeException(s"Failed to decode DR2 Event of type: $eventType"))

    val mockContext = MockitoSugar.mock[Context]
    MockitoSugar.when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))

    DR2EventHandler.handleEvent(ev)(mockContext.getLogger)
    ev
  }

}
