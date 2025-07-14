import Dependencies._

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "uk.gov.nationalarchives"

lazy val root = (project in file("."))
  .settings(
    name := "tdr-external-event-handling",
    libraryDependencies ++= Seq(
      awsLambdaCore,
      awsLambdaEvents,
      s3Utils,
      circeCore,
      circeGeneric,
      circeParser,
      typesafe,
      log4catsSlf4j,
      slf4jSimple,
      scalaTest % Test,
      wiremock % Test
    )
  )

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

(assembly / assemblyJarName) := "external-event-handling.jar"