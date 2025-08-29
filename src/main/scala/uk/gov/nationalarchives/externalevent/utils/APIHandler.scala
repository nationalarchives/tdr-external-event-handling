package uk.gov.nationalarchives.externalevent.utils

import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import sangria.ast.Document
import sttp.client3.{HttpClientFutureBackend, SttpBackend}
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.error.{GraphQlError, NotAuthorisedError}

import scala.concurrent.{ExecutionContext, Future}

object APIHandler {
  implicit val backend: SttpBackend[Future, Any] = HttpClientFutureBackend()

  def sendApiRequest[Data, Variables](
      graphQlClient: GraphQLClient[Data, Variables],
      document: Document,
      token: BearerAccessToken,
      variables: Variables
  )(implicit executionContext: ExecutionContext): Future[Data] = {
    val resp = graphQlClient
      .getResult(token, document, Some(variables))
      .flatMap(result =>
        result.errors match {
          case Nil                                 => Future.successful(result.data.get)
          case List(authError: NotAuthorisedError) => Future.failed(new AuthorisationException(authError.message))
          case errors                              => Future.failed(new GraphQlException(errors))
        })
    resp
  }
}

class AuthorisationException(message: String) extends Exception(message)
class GraphQlException(errors: List[GraphQlError]) extends RuntimeException(s"GraphQL response contained errors: ${errors.map(e => e.message).mkString}")
