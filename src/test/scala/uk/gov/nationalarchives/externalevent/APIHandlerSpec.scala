package uk.gov.nationalarchives.externalevent

import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import sangria.ast.Document
import sttp.client3.SttpBackend
import uk.gov.nationalarchives.tdr.GraphQLClient.Error
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}
import uk.gov.nationalarchives.tdr.error.GraphQlError
import uk.gov.nationalarchives.externalevent.utils.APIHandler.sendApiRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.should.Matchers._
import scala.reflect.ClassTag

class APIHandlerSpec extends AnyFlatSpec with MockitoSugar {
  "sendApiRequest" should "return the correct data if present" in {
    case class Data(test: Option[String])
    case class Variables()
    val client: GraphQLClient[Data, Variables] = mock[GraphQLClient[Data, Variables]]
    when(client.getResult[Future](any[BearerAccessToken], any[Document], any[Option[Variables]])(any[SttpBackend[Future, Any]], any[ClassTag[Future[_]]]))
      .thenReturn(Future(GraphQlResponse(Option(Data(Option("test"))), List())))
    val result: Data = sendApiRequest(client, new Document(Vector()), new BearerAccessToken("token"), Variables()).futureValue
    result.test.get should equal("test")
  }

  "sendApiRequest" should "return an error if the data is missing" in {
    case class Data(test: Option[String])
    case class Variables()
    val client: GraphQLClient[Data, Variables] = mock[GraphQLClient[Data, Variables]]
    when(client.getResult[Future](any[BearerAccessToken], any[Document], any[Option[Variables]])(any[SttpBackend[Future, Any]], any[ClassTag[Future[_]]]))
      .thenReturn(Future(GraphQlResponse(None, List(GraphQlError(Error("Error Message", Nil, Nil, None))))))
    val result: Throwable = sendApiRequest(client, new Document(Vector()), new BearerAccessToken("token"), Variables()).failed.futureValue
    result.getMessage should equal("GraphQL response contained errors: Error Message")
  }
}
