package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.api.client.logging.{LambdaContextLogger, StdOutLogSink}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.lambda.runtime.logging.{LogFormat, LogLevel}
import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.MockitoSugar._

import scala.jdk.CollectionConverters.SeqHasAsJava
import java.util.UUID

object TestUtils {

  private val testConfig: Config = ConfigFactory.load()

  val genericMessage = """  {
    "messageBody": "This is a generic message"
    }
  """.stripMargin

  val notJSON = "NotJson".stripMargin

  val randomUUID = UUID.randomUUID().toString

  val standardDR2Message = s"""
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
      }
  }
  """.stripMargin

  val nonStandardDR2Message = s"""
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
    }
  }
  """.stripMargin

  val incorrectDR2MessageType = s"""
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
    }
  }
  """.stripMargin

  val incorrectDR2Message1 = s"""
    {
      "body": {
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
        }
      },
      "timestamp": "1738339669217",
      "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
    }
    """.stripMargin

  val incorrectDR2Message2 = s"""
    {
      "body": {
        "properties": {
        "executionId": "TESTDOC_TDR-2021-CMTP_2",
        "messageId": "$randomUUID",
        "parentMessageId": null,
        "timestamp": "2025-01-31T16:07:49.129278081Z",
        },
        "parameters": {
        "assetId": "$randomUUID",
        "status": "Asset has been written to custodial copy disk."
        }
      },
      "timestamp": "1738339669217",
      "topicArn": "arn:aws:sns:eu-west-2:XXXXXXXXXXXX:intg-dr2-notifications"
    }
    """.stripMargin

  val expectedFileStatusResponse: String = s"""
    {
      "data": {
        "addMultipleFileStatuses": [
          {
            "fileId": "$randomUUID",
            "statusType": "${testConfig.getString("tags.dr2IngestKey")}",
            "statusValue": "${testConfig.getString("tags.dr2IngestValue")}"
          }
        ]
      }
    }
    """.stripMargin

  val expectedPutTagsRequestXml: String =
    s"""<?xml version="1.0" encoding="UTF-8"?><Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      |<TagSet><Tag><Key>${testConfig.getString("tags.dr2IngestKey")}</Key>
      |<Value>${testConfig.getString("tags.dr2IngestValue")}</Value></Tag></TagSet></Tagging>""".stripMargin.replaceAll("\\n", "")

  def unrecognisedPutTagsRequestXml(tagValue: String): String =
    s"""<?xml version="1.0" encoding="UTF-8"?><Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
       |<TagSet><Tag><Key>UnknownDR2Message</Key>
       |<Value>$tagValue</Value></Tag></TagSet></Tagging>""".stripMargin.replaceAll("\\n", "")

  def getSQSEvent(messages: List[String]): SQSEvent = {
    val testMessage = new SQSEvent
    testMessage.setRecords(messages.map(sqsMessage(_)).asJava)
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

  def mockContext: Context = {
    val mockContext = mock[Context]
    when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))
  }
}
