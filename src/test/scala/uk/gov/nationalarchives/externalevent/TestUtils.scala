package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import scala.jdk.CollectionConverters.SeqHasAsJava

import java.util.UUID

object TestUtils {
  val genericMessage = """  {
    "messageBody": "This is a generic message"
    }
  """.stripMargin

  val NotJSON = "FooBar".stripMargin

  val randomUUID = UUID.randomUUID().toString

  val StandardDR2Message = s"""
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

  val NonStandardDR2Message = s"""
  {
    "properties": {
      "executionId": "TESTDOC_TDR-2021-CMTP_1",
      "messageId": "$randomUUID",
      "parentMessageId": null,
      "timestamp": "2025-01-31T16:07:49.129278081Z",
      "messageType": "preserve.digital.asset.ingest.complete",
      "extraField": "This is an extra field not in the standard message"
    },
    "parameters": {
      "assetId": "$randomUUID",
      "status": "Asset has been written to custodial copy disk."
    },
    "timestamp": "1738339669217",
    "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
  }
  """.stripMargin

  val IncorrectDR2MessageType = s"""
  {
    "properties": {
      "executionId": "TESTDOC_TDR-2021-CMTP_0",
      "messageId": "$randomUUID",
      "parentMessageId": null,
      "timestamp": "2025-01-31T16:07:49.129278081Z",
      "messageType": "preserve.digital.asset.ingest.success"
    },
    "parameters": {
      "assetId": "$randomUUID",
      "status": "Asset has been written to custodial copy disk."
    },
    "timestamp": "1738339669217",
    "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
  }
  """.stripMargin

    val IncorrectDR2Message1 = s"""
    {
        "properties": {
        "executionId": "TESTDOC_TDR-2021-CMTP_2",
        "messageId": "$randomUUID",
        "parentMessageId": null,
        "timestamp": "2025-01-31T16:07:49.129278081Z",
        "messageType": "preserve.digital.asset.ingest.complete"
        },
        "details": {
        "assetId": "$randomUUID",
        "status": "Asset has been written to custodial copy disk."
        },
        "timestamp": "1738339669217",
        "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
    }
    """.stripMargin

  val IncorrectDR2Message2 = s"""
    {
        "properties": {
        "executionId": "TESTDOC_TDR-2021-CMTP_2",
        "messageId": "$randomUUID",
        "parentMessageId": null,
        "timestamp": "2025-01-31T16:07:49.129278081Z",
        },
        "parameters": {
        "assetId": "$randomUUID",
        "status": "Asset has been written to custodial copy disk."
        },
        "timestamp": "1738339669217",
        "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
    }
    """.stripMargin

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
