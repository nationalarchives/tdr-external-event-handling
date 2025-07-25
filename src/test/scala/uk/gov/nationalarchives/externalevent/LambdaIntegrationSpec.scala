package uk.gov.nationalarchives.externalevent

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.SeqHasAsJava
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import org.mockito.MockitoSugar

import TestUtils._

class LambdaIntegrationSpec extends AnyFlatSpec with MockitoSugar with Matchers {

  "The Lambda " should "run without throwing an exception" in {
    val message = new SQSEvent
    message.setRecords(List(sqsMessage(genericMessage)).asJava)

    new Lambda().handleRequest(message, mockContext)
  }
}
