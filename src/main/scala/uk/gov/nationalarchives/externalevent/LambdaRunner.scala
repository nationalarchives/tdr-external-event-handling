package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage

import scala.jdk.CollectionConverters.SeqHasAsJava

object LambdaRunner extends App {

  val testSQSMessage = s"""
                       |{
                       |  "Records": [
                       |        {
                       |            "messageId": "messageIdValue1",
                       |            "receiptHandle": "receipt handle value",
                       |            "body": "{\\"consignmentReference\\": \\"some-consignment-reference\\",\\"retryCount\\": 0}",
                       |            "attributes": {
                       |                "ApproximateReceiveCount": "1",
                       |                "SentTimestamp": "1545082649183",
                       |                "SenderId": "senderIdValue",
                       |                "ApproximateFirstReceiveTimestamp": "1545082649185"
                       |            },
                       |            "messageAttributes": {},
                       |            "md5OfBody": "md5OfBodyValue",
                       |            "eventSource": "aws:sqs",
                       |            "eventSourceARN": "queueArn",
                       |            "awsRegion": "eu-west-2"
                       |        }
                       |  ]
                       |}
                       |""".stripMargin

  val DR2SQSMessage1 = """
  {
    "properties": {
      "executionId": "TESTDOC_TDR-2021-CMTP_0",
      "messageId": "c4a1c1af-dd03-437d-b7e7-ba029d1afc4f",
      "parentMessageId": null,
      "timestamp": "2025-01-31T16:07:49.129278081Z",
      "messageType": "preserve.digital.asset.ingest.complete"
    },
    "parameters": {
      "assetId": "3581f148-1284-46a4-85eb-c3f14d396c24",
      "status": "Asset has been written to custodial copy disk."
    },
    "timestamp": "1738339669217",
    "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
  }
  """.stripMargin

  val DR2SQSMessage2 = """
  {
    "properties": {
      "executionId": "TESTDOC_TDR-2022-CMTP_2",
      "messageId": "cb8c1adc-b7c1-43db-a4cb-cd034a25186e ",
      "parentMessageId": null,
      "timestamp": "2025-03-01T16:07:49.129278081Z",
      "messageType": "preserve.digital.asset.ingest.complete"
    },
    "parameters": {
      "assetId": "102a7a41-4b33-4f65-bace-d92bc3915087",
      "status": "Asset has been written to custodial copy disk."
    },
    "timestamp": "1738339669217",
    "topicArn": "arn:aws:sqs:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
  }
  """.stripMargin

  val randommessage1 = """  {
    "messageBody": "This is Message 1"
    }
  """.stripMargin

  val randommessage2 = """  {
    "messageBody": "This is Message 2"
    }
  """.stripMargin

  val message1 = sqsMessage(DR2SQSMessage1)
  val message2 = sqsMessage(DR2SQSMessage2)

  val inputMessage = new SQSEvent
  inputMessage.setRecords((List(message1, message2).asJava))

  new Lambda().handleRequest(inputMessage, null)

  def sqsMessage(message: String): SQSMessage = {
    val sqsMessage = new SQSMessage
    sqsMessage.setBody(message)
    sqsMessage.setEventSourceArn("queueArn")
    sqsMessage.setEventSource("aws:sqs")
    sqsMessage.setAwsRegion("eu-west-2")
    sqsMessage
  }
}
