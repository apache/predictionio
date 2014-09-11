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

sparkVersion in ThisBuild := "1.0.2"

lazy val pioBuildInfoSettings = buildInfoSettings ++ Seq(
  sourceGenerators in Compile <+= buildInfo,
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    sparkVersion),
  buildInfoPackage := "io.prediction.core")

lazy val root = project in file(".") aggregate(
  core,
  data,
  engines,
  tools)

lazy val core = (project in file("core")).
  dependsOn(data).
  settings(genjavadocSettings: _*).
  settings(pioBuildInfoSettings: _*).
  enablePlugins(SbtTwirl)

lazy val data = (project in file("data"))

lazy val engines = (project in file("engines")).
  dependsOn(core).
  enablePlugins(SbtTwirl)

lazy val tools = (project in file("tools")).
  dependsOn(core).
  dependsOn(data).
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
    "html",
    "io.prediction.tools").mkString(":"),
  "-doc-title",
  "PredictionIO ScalaDoc",
  "-doc-version",
  version.value,
  "-doc-root-content",
  "docs/scaladoc/rootdoc.txt")

javacOptions in (JavaUnidoc, unidoc) := Seq(
  "-windowtitle",
  "PredictionIO Javadoc " + version.value,
  "-group",
  "Java Controllers",
  "io.prediction.controller.java",
  "-overview",
  "docs/javadoc/javadoc-overview.html",
  "-noqualifier",
  "java.lang")
