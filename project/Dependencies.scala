import sbt.*

object Dependencies {
  private val circeVersion = "0.14.14"
  private val awsUtilsVersion = "0.1.289"
  private val mockitoScalaVersion = "2.0.0"

  lazy val s3Utils = "uk.gov.nationalarchives" %% "s3-utils" % awsUtilsVersion
  lazy val awsLambdaCore = "com.amazonaws" % "aws-lambda-java-core" % "1.3.0"
  lazy val awsLambdaEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.16.1"
  lazy val awsSqs = "com.amazonaws" % "aws-java-sdk-sqs" % "1.12.432"
  lazy val awsLambdaRuntimeClient = "com.amazonaws" % "aws-lambda-java-runtime-interface-client" % "2.8.2"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val typesafe = "com.typesafe" % "config" % "1.4.3"
  lazy val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.17"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "3.0.1"
  lazy val mockitoScala = "org.mockito" %% "mockito-scala" % mockitoScalaVersion
  lazy val mockitoScalaTest = "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion
}
