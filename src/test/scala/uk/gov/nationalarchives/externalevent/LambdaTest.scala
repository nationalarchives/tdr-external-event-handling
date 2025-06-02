package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.jdk.CollectionConverters.SeqHasAsJava

import java.util.UUID

class LambdaTest extends AnyFlatSpec with Matchers {

  //TODO move messages and functions to a Utils object
  val genericMessage = """  {
    "messageBody": "This is a generic message"
    }
  """.stripMargin

  val randomUUID = UUID.randomUUID().toString

  val DR2Message = s"""
  {
    "properties": {
      "executionId": "TESTDOC_TDR-2021-CMTP_0",
      "messageId": "$randomUUID",
      "parentMessageId": null,
      "timestamp": "2025-01-31T16:07:49.129278081Z",
      "messageType": "preserve.digital.asset.ingest.complete"
    },
    "parameters": {
      "assetId": "$randomUUID",
      "status": "Asset has been written to custodial copy disk."
    },
    "timestamp": "1738339669217",
    "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
  }
  """.stripMargin

  "The lambda class" should "return a text output" in {
    val lambda = new Lambda()
    val message = getSQSEvent(DR2Message)
    val actual = lambda.handleRequest(message, null)
    actual shouldBe "Lambda has run"
  }

  def getSQSEvent(message: String): SQSEvent = {
    val testMessage = new SQSEvent
    testMessage.setRecords((List(sqsMessage(message)).asJava))
    testMessage
  }

  def sqsMessage(message: String): SQSMessage = {
    val sqsMessage = new SQSMessage
    sqsMessage.setBody(message)
    sqsMessage.setEventSourceArn("queueArn")
    sqsMessage.setEventSource("aws:sqs")
    sqsMessage.setAwsRegion("eu-west-2")
    sqsMessage
  }
}
