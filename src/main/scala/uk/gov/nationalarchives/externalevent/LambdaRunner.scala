package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import com.amazonaws.services.lambda.runtime.{ClientContext, CognitoIdentity, Context, LambdaLogger}
import org.slf4j.Logger
import org.slf4j.simple.SimpleLoggerFactory

import scala.jdk.CollectionConverters.SeqHasAsJava

object LambdaRunner extends App {

  val TestAssetID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"

  val DR2SQSMessage1 = s"""
  {
      "properties": {
        "executionId": "TESTDOC_TDR-2021-CMTP_0",
        "messageId": "c4a1c1af-dd03-437d-b7e7-ba029d1afc4f",
        "parentMessageId": null,
        "timestamp": "2025-01-31T16:07:49.129278081Z",
        "messageType": "preserve.digital.asset.ingest.update"
      },
      "parameters": {
        "assetId": "${TestAssetID}",
        "status": "Asset has been ingested to the Preservation System."
      }
  }
  """.stripMargin

  val DR2SQSMessage2 = s"""
  {
      "properties": {
        "executionId": "TESTDOC_TDR-2022-CMTP_0",
        "messageId": "cb8c1adc-b7c1-43db-a4cb-cd034a25186e ",
        "parentMessageId": null,
        "timestamp": "2025-03-01T16:07:49.129278081Z",
        "messageType": "preserve.digital.asset.ingest.complete"
      },
      "parameters": {
        "assetId": "${TestAssetID}",
        "status": "Asset has been written to custodial copy disk."
      }
  }
  """.stripMargin

  val message1 = sqsMessage(DR2SQSMessage1)
  val message2 = sqsMessage(DR2SQSMessage2)

  val inputMessage = new SQSEvent
  inputMessage.setRecords(List(message1, message2).asJava)

  new Lambda().handleRequest(inputMessage, context)

  def sqsMessage(message: String): SQSMessage = {
    val sqsMessage = new SQSMessage
    sqsMessage.setBody(message)
    sqsMessage.setEventSourceArn("queueArn")
    sqsMessage.setEventSource("aws:sqs")
    sqsMessage.setAwsRegion("eu-west-2")
    sqsMessage
  }

  private def context: Context = new Context {
    override def getAwsRequestId: String = ""
    override def getLogGroupName: String = ""
    override def getLogStreamName: String = ""
    override def getFunctionName: String = ""
    override def getFunctionVersion: String = ""
    override def getInvokedFunctionArn: String = ""
    override def getIdentity: CognitoIdentity = null
    override def getClientContext: ClientContext = null
    override def getRemainingTimeInMillis: Int = 1
    override def getMemoryLimitInMB: Int = 1
    override def getLogger: LambdaLogger = TestLogger
  }

  def TestLogger: LambdaLogger = new LambdaLogger {
    val logger: Logger = new SimpleLoggerFactory().getLogger(this.getClass.getName)

    override def log(message: String): Unit = {
      logger.info(message)
    }

    override def log(message: Array[Byte]): Unit = {
      logger.info(new String(message))
    }

    override def log(message: String, logLevel: LogLevel): Unit = {
      logLevel match {
        case LogLevel.DEBUG => logger.debug(message)
        case LogLevel.INFO  => logger.info(message)
        case LogLevel.WARN  => logger.warn(message)
        case LogLevel.ERROR => logger.error(message)
        case LogLevel.TRACE => logger.trace(message)
      }
    }
  }
}
