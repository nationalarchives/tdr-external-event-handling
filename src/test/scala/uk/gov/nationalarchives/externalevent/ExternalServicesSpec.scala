package uk.gov.nationalarchives.externalevent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, containing, equalTo, get, ok, okJson, post, put, serverError, urlEqualTo, urlMatching, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import TestUtils._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

class ExternalServicesSpec extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  val wiremockS3 = new WireMockServer(8003)
  val expectedFileStatusResponseTransformer = new ExpectedFileStatusResponseTransformer()
  val wiremockGraphql = new WireMockServer(
    wireMockConfig()
      .port(9001)
      .extensions(expectedFileStatusResponseTransformer)
  )  
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

  def mockS3ListResponse(prefix: String, files: List[String]): StubMapping = {
    val fullKeys = if (prefix.isEmpty) files else files.map(f => s"$prefix/$f") ++ Seq(s"$prefix/$prefix.metadata")
    val listResponseXml =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
         |  ${fullKeys.map(key => s"<Contents><Key>$key</Key></Contents>").mkString("\n")}
         |</ListBucketResult>""".stripMargin

    wiremockS3.stubFor(
      get(urlPathEqualTo("/"))
        .withQueryParam("list-type", equalTo("2"))
        .willReturn(ok(listResponseXml).withHeader("Content-Type", "application/xml"))
    )
  }
  def mockS3GetTaggingResponse(): StubMapping = {
    wiremockS3.stubFor(
      get(urlMatching(".*\\?tagging"))
        .willReturn(ok("<Tagging></Tagging>").withHeader("Content-Type", "application/xml"))
    )
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
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("""{"data":{"addMultipleFileStatuses":[{"fileId":"{{jsonPath request.body '$.variables.input[0].fileId'}}","statusType":"PreserveDigitalAssetIngest","statusValue":"Complete"}]}}""")
        .withTransformers("expected-file-status-response-transformer")))
  }

  def authOk(): StubMapping = wiremockAuth.stubFor(post(urlEqualTo(authPath))
    .willReturn(okJson("""{"access_token": "valid-token"}""")))

  def authUnavailable(): StubMapping = wiremockAuth.stubFor(post(urlEqualTo(authPath)).willReturn(serverError()))
  def graphqlUnavailable(): StubMapping = wiremockGraphql.stubFor(post(urlEqualTo(graphQlPath)).willReturn(serverError()))
}