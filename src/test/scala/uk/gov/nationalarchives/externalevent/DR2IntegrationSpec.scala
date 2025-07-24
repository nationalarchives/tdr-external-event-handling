package uk.gov.nationalarchives.externalevent

import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.parser.decode
import com.typesafe.config.{Config, ConfigFactory}
import uk.gov.nationalarchives.externalevent.decoders._
import uk.gov.nationalarchives.externalevent.events._
import TestUtils._
import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.api.client.logging.{LambdaContextLogger, StdOutLogSink}
import com.amazonaws.services.lambda.runtime.logging.{LogFormat, LogLevel}
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse
import uk.gov.nationalarchives.aws.utils.s3.S3Utils

class DR2IntegrationSpec extends ExternalServicesSpec with MockitoSugar with Matchers {

  "x" should "y" in {
    mockS3GetResponse()
    mockS3PutResponse()
    val mockContext = mock[Context]
    when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))

    val event:DR2EventDecoder.DR2Event = decode[DR2EventDecoder.DR2Event](sqsMessage(StandardDR2Message).getBody)
      .getOrElse(throw new RuntimeException("Failed to decode DR2 Event"))

    DR2EventHandler.handleEvent(event)(mockContext.getLogger)
  }


  "The Lambda" should "try to tag an S3 Object when a DR2 Message is passed" in {

    val s3Utils = mock[S3Utils]
    val mockContext = mock[Context]
    when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))

    val event:DR2EventDecoder.DR2Event = decode[DR2EventDecoder.DR2Event](sqsMessage(StandardDR2Message).getBody)
      .getOrElse(throw new RuntimeException("Failed to decode DR2 Event"))

    val bucketCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val keyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val tagsCaptor: ArgumentCaptor[Map[String, String]] = ArgumentCaptor.forClass(classOf[Map[String, String]])
    //val mockResponse = IO.pure(PutObjectTaggingResponse.builder.build())
    //doAnswer(() => mockResponse).when(s3Utils).addObjectTags(bucketCaptor.capture(), keyCaptor.capture(), tagsCaptor.capture())

    when(s3Utils.addObjectTags(bucketCaptor.capture(), keyCaptor.capture(), tagsCaptor.capture())).thenReturn(IO.pure(PutObjectTaggingResponse.builder.build()))
    //verify(s3Utils).addObjectTags(bucketCaptor.capture(), keyCaptor.capture(), tagsCaptor.capture())

    DR2EventHandler.handleEvent(event)(mockContext.getLogger)

    println(s"Bucket: ${bucketCaptor.getValue}")
    //println(s"Key: ${keyCaptor.getValue}")
    //println(s"Tags: ${tagsCaptor.getValue}")

  }

  /*
 "The Lambda " should "try to tag an S3 Object when a DR2 Message is passed" in {

   val DR2EventCaptor: ArgumentCaptor[DR2EventDecoder.DR2Event] = ArgumentCaptor.forClass(classOf[DR2EventDecoder.DR2Event])
   //val bucketCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
   //val keyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
   //val tagCaptor: ArgumentCaptor[Map[String, String]] = ArgumentCaptor.forClass(classOf[Map[String, String]])

   //val s3AsyncClient = mock[S3AsyncClient]
   //println(s"Class: ${DR2EventCaptor.getValue.getClass.getName}")

   val mockHandler = mock[DR2EventHandler]

   val lambda = new Lambda()

   val message = new SQSEvent
   message.setRecords(List(TestUtils.sqsMessage(TestUtils.DR2Message)).asJava)

   val mockContext = mock[Context]
   when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))
   doAnswer(() => true).when(mockHandler).handleEvent(DR2EventCaptor.capture())

   lambda.handleRequest(message, mockContext)

   //println(s"Class: ${DR2EventCaptor.getValue.getClass.getName}")


   //Mockito.verify(mockHandler).handleEvent(DR2EventCaptor.capture())
   //

   //println(s"Class: ${DR2EventCaptor.getValue.getClass.getName}")

   //
   //println(s"Class: ${DR2EventCaptor.getClass.getName}")

 }



  val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080))
  wireMockServer.start()

  //From tdr-transfer-frontend DownloadServiceSpec

  val s3Endpoint = "https://mock-s3-endpoint.com"
  val s3AsyncClient: S3AsyncClient = mock[S3AsyncClient]
  when(mockAppConfig.s3Endpoint).thenReturn(s3Endpoint)

  wireMockServer.stubFor(put(urlMatching("/my-bucket/my-object\\?tagging"))
    .willReturn(aResponse().withStatus(200)))

  val s3AsyncClient = mock[S3AsyncClient]
  val s3Utils = S3Utils(s3AsyncClient)

  val s3Client = S3AsyncClient.builder()
    .endpointOverride(URI.create("http://localhost:8080"))
    .build()

  "The lambda class" should "return a text output" in {
    val lambda = new Lambda()
    val message = TestUtils.getSQSEvent(TestUtils.DR2Message)
    val actual = lambda.handleRequest(message, null)
  }

  "S3Utils" should "add tags to an object using WireMock" in {
    // Call your tagging method here
    val result = s3Utils.addObjectTags("my-bucket", "my-object", Map("tag1" -> "value1")).unsafeRunSync()
    println(s"Result: $result")
    result shouldBe ()
    wireMockServer.verify(putRequestedFor(urlMatching("/my-bucket/my-object\\?tagging")))
  }

  wireMockServer.stop()


   */

}
