package uk.gov.nationalarchives.externalevent

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.typesafe.config.Config
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}
import graphql.codegen.AddMultipleFileStatuses.{addMultipleFileStatuses => amfs}
import graphql.codegen.types._
import uk.gov.nationalarchives.aws.utils.ssm.{SSMClients, SSMUtils}

import java.util.UUID
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt
import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend}
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment}
import uk.gov.nationalarchives.externalevent.utils.APIHandler.sendApiRequest

class GraphQlApi(val config: Config, val keycloak: KeycloakUtils, addFileStatusClient: GraphQLClient[amfs.Data, amfs.Variables])(implicit
    val logger: LambdaLogger,
    keycloakDeployment: TdrKeycloakDeployment,
    backend: SttpBackend[Identity, Any]
) {
  implicit class ErrorUtils[D](response: GraphQlResponse[D]) { val errorString: String = response.errors.map(_.message).mkString("\n")}

  private val ssmUtils = SSMUtils(SSMClients.ssm(config.getString("ssm.endpoint")))

  def updateFileStatus(assetId: String, statusType: String, statusValue: String)(implicit ec: ExecutionContext): amfs.Data = {
    println("UpdateFileStatus")
    val result = for {
      token <- keycloak.serviceAccountToken(config.getString("auth.clientId"), ssmUtils.getParameterValue(config.getString("auth.clientSecretPath")))
      _ = println("Creating fileStatusInput")
      fileStatusInput = AddMultipleFileStatusesInput(List(AddFileStatusInput(UUID.fromString(assetId), statusType, statusValue)))
      _ = println("Calling sendApiRequest")
      response <- sendApiRequest(addFileStatusClient, amfs.document, token, amfs.Variables(fileStatusInput))
    } yield response
    Await.result(result, 5.seconds)
  }
}

object GraphQlApi {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  def apply(config: Config)(implicit logger: LambdaLogger): GraphQlApi = {
    val apiUrl = config.getString("api.url")
    val keycloak = new KeycloakUtils()
    val keycloakDeployment: TdrKeycloakDeployment = TdrKeycloakDeployment(config.getString("auth.url"), "tdr", 60)
    val addFileStatusClient = new GraphQLClient[amfs.Data, amfs.Variables](apiUrl)
    new GraphQlApi(config, keycloak, addFileStatusClient)(logger, keycloakDeployment, backend)
  }
}
