import Dependencies._

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "uk.gov.nationalarchives"

lazy val root = (project in file("."))
  .settings(
    name := "tdr-external-event-handling"
  )