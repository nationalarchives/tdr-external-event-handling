import sbt.*

object Dependencies {
  private val circeVersion = "0.14.14"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "3.0.1"
  lazy val awsLambdaCore = "com.amazonaws" % "aws-lambda-java-core" % "1.3.0"
  lazy val awsLambdaEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.16.1"
  lazy val awsSqs = "com.amazonaws" % "aws-java-sdk-sqs" % "1.12.432"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
}
