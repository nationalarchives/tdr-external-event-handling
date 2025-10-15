package uk.gov.nationalarchives.externalevent

import org.scalatest.matchers.should.Matchers
import io.circe.parser.decode
import uk.gov.nationalarchives.externalevent.decoders._
import uk.gov.nationalarchives.externalevent.events._
import com.github.tomakehurst.wiremock.client.WireMock._

import scala.jdk.CollectionConverters.ListHasAsScala
import TestUtils._

class DR2IntegrationSpec extends ExternalServicesSpec with Matchers {

  "Lambda" should "correctly handle a single DR2 message" in {
    mockS3ListResponse(prefix = prefixUUID, files = List(file1UUID, file2UUID, file3UUID))
    mockS3GetTaggingResponse()
    mockS3PutResponse()
    authOk()
    graphqlOkJson()

    new Lambda().handleRequest(getSQSEvent(List(standardDR2Message)), mockContext)
    
    wiremockS3.verify(
      putRequestedFor(urlMatching(".*\\?tagging"))
        .withRequestBody(equalToXml(expectedPutTagsRequestXml))
    )
    wiremockS3.verify(
      4,
      putRequestedFor(urlMatching(".*\\?tagging"))
    )
  }

  "Lambda" should "correctly handle multiple DR2 messages" in {
    mockS3ListResponse(prefix = prefixUUID, files = List(file1UUID, file2UUID, file3UUID))
    mockS3GetTaggingResponse()
    mockS3PutResponse()
    authOk()
    graphqlOkJson()

    new Lambda().handleRequest(getSQSEvent(List(standardDR2Message, nonStandardDR2Message)), mockContext)

    wiremockS3.verify(
      putRequestedFor(urlMatching(".*\\?tagging"))
        .withRequestBody(equalToXml(expectedPutTagsRequestXml))
    )
    wiremockS3.verify(
      8,
      putRequestedFor(urlMatching(".*\\?tagging"))
    )
  }
  

  "DR2EventHandler" should "apply tags to all files at location including metadata file on receipt of a standard DR2 message" in {
    val files = List(file1UUID, file2UUID, file3UUID)
    mockS3ListResponse(prefix = prefixUUID, files = List(file1UUID, file2UUID, file3UUID))
    mockS3GetTaggingResponse()
    mockS3PutResponse()
    authOk()
    graphqlOkJson()
    val ev = runEventHandler(standardDR2Message)
    
    (files ++ Seq(s"$prefixUUID.metadata")).foreach { file =>
      wiremockS3.verify(
        putRequestedFor(urlMatching(s"/${ev.assetId}/$file\\?tagging"))
          .withRequestBody(equalToXml(expectedPutTagsRequestXml))
      )
    }
  }

  "DR2EventHandler" should "apply tags to all files at location including metadata file on receipt of a non-standard DR2 message" in {
    val files = List(file1UUID, file2UUID, file3UUID)
    mockS3ListResponse(prefix = prefixUUID, files = List(file1UUID, file2UUID, file3UUID))
    mockS3GetTaggingResponse()
    mockS3PutResponse()
    authOk()
    graphqlOkJson()
    val ev = runEventHandler(nonStandardDR2Message)

    (files ++ Seq(s"$prefixUUID.metadata")).foreach { file =>
      wiremockS3.verify(
        putRequestedFor(urlMatching(s"/${ev.assetId}/$file\\?tagging"))
          .withRequestBody(equalToXml(expectedPutTagsRequestXml))
      )
    }
  }
  
  "DR2EventHandler" should "pass alternative tags when a DR2 Message has an unrecognised message type" in {
    val files = List(file1UUID, file2UUID, file3UUID)
    mockS3ListResponse(prefix = prefixUUID, files = List(file1UUID, file2UUID, file3UUID))
    mockS3GetTaggingResponse()
    mockS3PutResponse()
    authOk()
    graphqlOkJson()
    
    val ev = runEventHandler(incorrectDR2MessageType)

    (files ++ Seq(s"$prefixUUID.metadata")).foreach { file =>
      wiremockS3.verify(
        putRequestedFor(urlMatching(s"/${ev.assetId}/$file\\?tagging"))
          .withRequestBody(equalToXml(unrecognisedPutTagsRequestXml(ev.messageType)))
      )
    }
  }

  "DR2EventHandler" should "send file status updates to the API for all files except metadata" in {
    val files = List(file1UUID, file2UUID, file3UUID)
    mockS3ListResponse(prefix = prefixUUID, files = List(file1UUID, file2UUID, file3UUID))
    mockS3GetTaggingResponse()
    mockS3PutResponse()
    authOk()
    graphqlOkJson()
    runEventHandler(standardDR2Message, true)

    val serveEvents = wiremockGraphql.getAllServeEvents.asScala
    val fileStatusUpdates = serveEvents.filter(_.getRequest.getBodyAsString.contains("addMultipleFileStatuses"))
    fileStatusUpdates.size should be (3)
    files.foreach(file => fileStatusUpdates.count(_.getRequest.getBodyAsString.contains(file)) should be (1))
  }

  def runEventHandler(eventType: String, updateFileStatus: Boolean = false): DR2EventDecoder.DR2Event = {
    val ev = decode[DR2EventDecoder.DR2Event](sqsMessage(eventType).getBody)
      .getOrElse(throw new RuntimeException(s"Failed to decode DR2 Event of type: $eventType"))

    DR2EventHandler.handleEvent(ev, updateFileStatus)(mockContext.getLogger)
    ev
  }
}