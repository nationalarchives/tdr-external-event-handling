package uk.gov.nationalarchives.externalevent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{anyUrl, get, ok, put}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

class ExternalServicesSpec extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(100, Millis)))
  val wiremockS3 = new WireMockServer(8003)

  override def beforeAll(): Unit = {
    wiremockS3.start()
  }

  override def afterAll(): Unit = {
    wiremockS3.stop()
  }

  override def afterEach(): Unit = {
    println("Resetting WireMock S3 server")
    wiremockS3.resetAll()
  }

  def mockS3GetResponse(): StubMapping = {
    wiremockS3.stubFor(get(anyUrl()).willReturn(ok()))
  }

  def mockS3PutResponse(): StubMapping = {
    wiremockS3.stubFor(put(anyUrl()).willReturn(ok()))
  }
}
