package uk.gov.nationalarchives.externalevent

import org.scalatest.matchers.should.Matchers
import io.circe.parser.decode
import uk.gov.nationalarchives.externalevent.decoders._
import uk.gov.nationalarchives.externalevent.events._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import com.typesafe.config.ConfigFactory
import org.mockito.MockitoSugar.{mock, verify}
import org.mockito.ArgumentMatchers.{contains, eq => eqTo}

import java.util.UUID
import scala.jdk.CollectionConverters.ListHasAsScala
import TestUtils._

class DR2IntegrationSpec extends ExternalServicesSpec with Matchers {

  private val config = ConfigFactory.load()
  private val exportBucket = config.getString("s3.standardBucket")
  private val dirtyBucket = config.getString("s3.dirtyBucket")
  private val cleanBucket = config.getString("s3.cleanBucket")
  private val quarantineBucket = config.getString("s3.quarantineBucket")
  private val consignmentTag = config.getString("tags.consignmentId")
  private val userTag = config.getString("tags.userId")
  private val sourceTag = config.getString("tags.assetSource")
  private val consignmentId = UUID.randomUUID().toString
  private val userId = UUID.randomUUID().toString
  private val contextTags = Map(consignmentTag -> consignmentId, userTag -> userId)

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

    wiremockS3.verify(
      putRequestedFor(urlMatching(s"/${ev.assetId}.metadata\\?tagging"))
        .withRequestBody(equalToXml(expectedPutTagsRequestXml))
    )

    files.foreach { file =>
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

    wiremockS3.verify(
      putRequestedFor(urlMatching(s"/${ev.assetId}.metadata\\?tagging"))
        .withRequestBody(equalToXml(expectedPutTagsRequestXml))
    )

    files.foreach { file =>
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

    wiremockS3.verify(
      putRequestedFor(urlMatching(s"/${ev.assetId}.metadata\\?tagging"))
        .withRequestBody(equalToXml(unrecognisedPutTagsRequestXml(ev.messageType)))
    )

    files.foreach { file =>
      wiremockS3.verify(
        putRequestedFor(urlMatching(s"/${ev.assetId}/$file\\?tagging"))
          .withRequestBody(equalToXml(unrecognisedPutTagsRequestXml(ev.messageType)))
      )
    }
  }

  "DR2EventHandler" should "send file status updates to the API for asset" in {
    val fileIds = List(file1UUID, file2UUID, file3UUID)
    mockS3ListResponse(prefix = prefixUUID, files = fileIds)
    mockS3GetTaggingResponse()
    mockS3PutResponse()
    authOk()
    graphqlOkJson()
    runEventHandler(standardDR2Message, true)

    val serveEvents = wiremockGraphql.getAllServeEvents.asScala
    val fileStatusUpdates = serveEvents.filter(_.getRequest.getBodyAsString.contains("addMultipleFileStatuses"))
    fileStatusUpdates.size should be(fileIds.size)
  }

  for (source <- List(None, Some("SharePoint"))) {
    "DR2EventHandler" should s"tag upstream record copies and preserve their tags with asset source $source" in {
      val files = List(file1UUID, file2UUID)
      val exportTags = contextTags ++ source.map(sourceTag -> _)
      mockS3ListResponse(prefixUUID, files)
      mockS3GetTaggingResponse(Map("ExistingTag" -> "existing-value"))
      mockS3GetTaggingResponse(exportTags, Some(exportBucket))
      mockS3PutResponse()
      authOk()
      graphqlOkJson()

      runEventHandler(standardDR2Message, updateFileStatus = true)

      files.foreach { file =>
        val dirtyKey = (List(userId) ++ source ++ List(consignmentId, file)).mkString("/")
        List(dirtyBucket -> dirtyKey, cleanBucket -> s"$consignmentId/$file", quarantineBucket -> s"$consignmentId/$file").foreach {
          case (bucket, key) =>
            wiremockS3.verify(
              1,
              putRequestedFor(urlEqualTo(s"/$key?tagging"))
                .withHeader("Host", equalTo(s"$bucket.localhost:8003"))
                .withRequestBody(matchingXPath(tagWithValue("PreserveDigitalAssetIngest", "Complete")))
                .withRequestBody(matchingXPath(tagWithValue("ExistingTag", "existing-value")))
                .withRequestBody(notMatching(".*TestConsignmentId.*"))
            )
        }
        wiremockS3.verify(
          putRequestedFor(urlEqualTo(s"/$prefixUUID/$file?tagging"))
            .withHeader("Host", equalTo(s"$exportBucket.localhost:8003"))
            .withRequestBody(matchingXPath(tagWithValue("PreserveDigitalAssetIngest", "Complete")))
            .withRequestBody(matchingXPath(tagWithValue(consignmentTag, consignmentId)))
            .withRequestBody(matchingXPath(tagWithValue(userTag, userId)))
        )
      }
      // Only exported records and the sidecar, plus three upstream copies per record.
      wiremockS3.verify(9, putRequestedFor(urlMatching(".*\\?tagging")))
      wiremockS3.verify(
        putRequestedFor(urlEqualTo(s"/$prefixUUID.metadata?tagging"))
          .withHeader("Host", equalTo(s"$exportBucket.localhost:8003"))
          .withRequestBody(matchingXPath(tagWithValue(consignmentTag, consignmentId)))
          .withRequestBody(matchingXPath(tagWithValue(userTag, userId)))
      )
      val fileStatusUpdates = wiremockGraphql.getAllServeEvents.asScala.filter(_.getRequest.getBodyAsString.contains("addMultipleFileStatuses"))
      fileStatusUpdates.size should be(files.size)
    }
  }

  for (missingContext <- List(Map.empty[String, String], Map(consignmentTag -> consignmentId), Map(userTag -> userId), contextTags.updated(userTag, ""))) {
    "DR2EventHandler" should s"keep tagging exports but warn and skip upstream copies when context is incomplete: $missingContext" in {
      val logger = mock[LambdaLogger]
      mockS3ListResponse(prefixUUID, List(file1UUID))
      mockS3GetTaggingResponse(missingContext)
      mockS3PutResponse()

      runEventHandler(standardDR2Message, logger = logger)

      wiremockS3.verify(2, putRequestedFor(urlMatching(".*\\?tagging")).withHeader("Host", equalTo(s"$exportBucket.localhost:8003")))
      wiremockS3.verify(2, putRequestedFor(urlMatching(".*\\?tagging")))
      verify(logger).log(contains("missing"), eqTo(LogLevel.WARN))
    }
  }

  "DR2EventHandler" should "not tag upstream objects for an unrecognised message type even when context is available" in {
    mockS3ListResponse(prefixUUID, List(file1UUID))
    mockS3GetTaggingResponse(contextTags)
    mockS3PutResponse()

    runEventHandler(incorrectDR2MessageType)

    wiremockS3.verify(2, putRequestedFor(urlMatching(".*\\?tagging")).withHeader("Host", equalTo(s"$exportBucket.localhost:8003")))
    wiremockS3.verify(2, putRequestedFor(urlMatching(".*\\?tagging")))
  }

  for (
    (status, code, level) <- List((404, "NoSuchKey", LogLevel.WARN), (403, "AccessDenied", LogLevel.ERROR), (404, "NoSuchBucket", LogLevel.ERROR));
    operation <- List("GET", "PUT")
  ) {
    "DR2EventHandler" should s"log $code from $operation and continue tagging the other upstream copies" in {
      val logger = mock[LambdaLogger]
      mockS3ListResponse(prefixUUID, List(file1UUID))
      mockS3GetTaggingResponse()
      mockS3GetTaggingResponse(contextTags, Some(exportBucket))
      mockS3PutResponse()
      wiremockS3.stubFor(
        request(operation, urlMatching(".*\\?tagging"))
          .withHeader("Host", equalTo(s"$dirtyBucket.localhost:8003"))
          .willReturn(aResponse().withStatus(status).withHeader("Content-Type", "application/xml").withBody(s"<Error><Code>$code</Code></Error>"))
      )

      runEventHandler(standardDR2Message, logger = logger)

      List(cleanBucket, quarantineBucket).foreach { bucket =>
        wiremockS3.verify(
          putRequestedFor(urlEqualTo(s"/$consignmentId/$file1UUID?tagging"))
            .withHeader("Host", equalTo(s"$bucket.localhost:8003"))
        )
      }
      wiremockS3.verify(
        if (operation == "PUT") 1 else 0,
        putRequestedFor(urlMatching(".*\\?tagging")).withHeader("Host", equalTo(s"$dirtyBucket.localhost:8003"))
      )
      verify(logger).log(contains(s"s3://$dirtyBucket/"), eqTo(level))
    }
  }

  private def tagWithValue(key: String, value: String): String =
    s"//*[local-name()='Tag'][*[local-name()='Key']='$key' and *[local-name()='Value']='$value']"

  def runEventHandler(eventType: String, updateFileStatus: Boolean = false, logger: LambdaLogger = mockContext.getLogger): DR2EventDecoder.DR2Event = {
    val ev = decode[DR2EventDecoder.DR2Event](sqsMessage(eventType).getBody)
      .getOrElse(throw new RuntimeException(s"Failed to decode DR2 Event of type: $eventType"))

    DR2EventHandler.handleEvent(ev, updateFileStatus)(logger)
    ev
  }
}
