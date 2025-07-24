package uk.gov.nationalarchives.externalevent

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.SeqHasAsJava
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.api.client.logging.{LambdaContextLogger, StdOutLogSink}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.logging.{LogFormat, LogLevel}
import org.mockito.MockitoSugar

import TestUtils._

class LambdaIntegrationSpec extends AnyFlatSpec with MockitoSugar with Matchers {

  "The Lambda " should "run without throwing an exception" in {
    val message = new SQSEvent
    message.setRecords(List(sqsMessage(genericMessage)).asJava)

    val mockContext = mock[Context]
    when(mockContext.getLogger).thenReturn(new LambdaContextLogger(new StdOutLogSink, LogLevel.ERROR, LogFormat.JSON))

    new Lambda().handleRequest(message, mockContext)
  }

}
