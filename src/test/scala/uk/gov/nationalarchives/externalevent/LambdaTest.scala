package uk.gov.nationalarchives.externalevent

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LambdaTest extends AnyFlatSpec with Matchers {

  "The lambda class" should "return a text output" in {
    val lambda = new Lambda()
    val actual = lambda.process()
    actual shouldBe "External Event Handling Lambda called"  }

}
