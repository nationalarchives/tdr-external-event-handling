import Dependencies._

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "uk.gov.nationalarchives"

lazy val root = (project in file("."))
  .settings(
    name := "tdr-external-event-handling",
    libraryDependencies ++= Seq(
      s3Utils,
      awsLambdaCore,
      awsLambdaEvents,
      awsLambdaRuntimeClient,
      circeCore,
      circeGeneric,
      circeParser,
      typesafe,
      slf4jSimple,
      scalaTest % Test,
      wiremock % Test,
      mockitoScala % Test,
      mockitoScalaTest % Test

    )
  )

(Test / fork) := true
(Test / javaOptions) += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf"
(Test / envVars) := Map("AWS_ACCESS_KEY_ID" -> "test", "AWS_SECRET_ACCESS_KEY" -> "test")

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

(assembly / assemblyJarName) := "external-event-handling.jar"