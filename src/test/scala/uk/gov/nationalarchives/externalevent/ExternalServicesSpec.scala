package uk.gov.nationalarchives.externalevent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{anyUrl, containing, get, ok, okJson, post, put, serverError, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import TestUtils._



class ExternalServicesSpec extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  val wiremockS3 = new WireMockServer(8003)
  val wiremockGraphql = new WireMockServer(9001)
  val wiremockAuth = new WireMockServer(9002)
  val wiremockSsm = new WireMockServer(9003)

  val graphQlPath = "/graphql"
  val authPath = "/auth/realms/tdr/protocol/openid-connect/token"

  override def beforeAll(): Unit = {
    wiremockS3.start()
    wiremockSsm.start()
    wiremockGraphql.start()
    wiremockAuth.start()
  }

  override def afterAll(): Unit = {
    wiremockS3.stop()
    wiremockSsm.stop()
    wiremockGraphql.stop()
    wiremockAuth.stop()
  }

  override def beforeEach(): Unit = {
    setUpSsmServer()
  }

  override def afterEach(): Unit = {
    wiremockS3.resetAll()
    wiremockSsm.resetAll()
    wiremockGraphql.resetAll()
    wiremockAuth.resetAll()
  }

  def mockS3GetResponse(): StubMapping = {
    wiremockS3.stubFor(get(anyUrl()).willReturn(ok()))
  }

  def mockS3PutResponse(): StubMapping = {
    wiremockS3.stubFor(put(anyUrl()).willReturn(ok()))
  }

  def setUpSsmServer(): StubMapping = {
    wiremockSsm
      .stubFor(
        post(urlEqualTo("/")).willReturn(okJson("""{"Parameter" : {"Name":"string","Value":"string"}}"""))
      )
  }

  def graphqlOkJson(): StubMapping = {
    wiremockGraphql.stubFor(post(urlEqualTo(graphQlPath))
      .withRequestBody(containing("addMultipleFileStatuses"))
      .willReturn(okJson(expectedFileStatusResponse)))
  }

  def authOk(): StubMapping = wiremockAuth.stubFor(post(urlEqualTo(authPath))
    .willReturn(okJson("""{"access_token": "valid-token"}""")))

  def authUnavailable(): StubMapping = wiremockAuth.stubFor(post(urlEqualTo(authPath)).willReturn(serverError()))
  def graphqlUnavailable(): StubMapping = wiremockGraphql.stubFor(post(urlEqualTo(graphQlPath)).willReturn(serverError()))
}
