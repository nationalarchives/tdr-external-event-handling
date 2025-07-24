package uk.gov.nationalarchives.externalevent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{anyUrl, ok, put, get}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class ExternalServicesSpec extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  val wiremockS3 = new WireMockServer(8003)

  override def beforeAll(): Unit = {
    wiremockS3.start()
  }

  override def afterAll(): Unit = {
    wiremockS3.stop()
  }

  override def afterEach(): Unit = {
    wiremockS3.resetAll()
  }

  //Can use this method to simulate responses from S3 for the get tag request
  def mockS3GetResponse(): StubMapping = {
    wiremockS3.stubFor(get(anyUrl()).willReturn(ok()))
  }

  //Can use this method to simulate responses from S3 for the put tag request
  def mockS3PutResponse(): StubMapping = {
    wiremockS3.stubFor(put(anyUrl()).willReturn(ok()))
  }
}
