import UnidocKeys._

name := "pio"

version in ThisBuild := "0.8.0-SNAPSHOT"

organization in ThisBuild := "io.prediction"

scalaVersion in ThisBuild := "2.10.4"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions in (ThisBuild, Test) ++= Seq("-Yrangepos")

fork in (ThisBuild, run) := true

javacOptions in ThisBuild ++= Seq("-source", "1.7", "-target", "1.7",
  "-Xlint:deprecation", "-Xlint:unchecked")

lazy val pioBuildInfoSettings = buildInfoSettings ++ Seq(
  sourceGenerators in Compile <+= buildInfo,
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "io.prediction")

lazy val root = project in file(".") aggregate(
  core,
  tools)

lazy val core = (project in file("core")).
  settings(genjavadocSettings: _*).
  settings(pioBuildInfoSettings: _*).
  enablePlugins(SbtTwirl)

lazy val tools = (project in file("tools")).
  dependsOn(core).
  enablePlugins(SbtTwirl)

scalaJavaUnidocSettings

unidocAllSources in (JavaUnidoc, unidoc) := {
  (unidocAllSources in (JavaUnidoc, unidoc)).value
    .map(_.filterNot(_.getName.contains("$")))
    .map(_.filterNot(_.getCanonicalPath.contains("engines")))
}

scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
  "-groups",
  "-skip-packages",
  Seq(
    "akka",
    "breeze",
    "io.prediction.engines",
    "myengine",
    "org").mkString(":"),
  "-doc-title",
  "PredictionIO ScalaDoc",
  "-doc-version",
  version.value,
  "-doc-root-content",
  "docs/rootdoc.txt")

javacOptions in (JavaUnidoc, unidoc) := Seq(
  "-windowtitle",
  "PredictionIO Javadoc " + version.value,
  "-group",
  "Java Controllers",
  "io.prediction.controller.java",
  "-overview",
  "docs/javadoc-overview.html",
  "-noqualifier",
  "java.lang")
