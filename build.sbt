// Copyright 2015 TappingStone, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import SonatypeKeys._

import UnidocKeys._

name := "pio"

version in ThisBuild := "0.9.3-SNAPSHOT"

organization in ThisBuild := "io.prediction"

scalaVersion in ThisBuild := "2.10.4"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions in (ThisBuild, Test) ++= Seq("-Yrangepos")

fork in (ThisBuild, run) := true

javacOptions in (ThisBuild, compile) ++= Seq("-source", "1.7", "-target", "1.7",
  "-Xlint:deprecation", "-Xlint:unchecked")

elasticsearchVersion in ThisBuild := "1.4.4"

json4sVersion in ThisBuild := "3.2.10"

sparkVersion in ThisBuild := "1.3.0"

lazy val pioBuildInfoSettings = buildInfoSettings ++ Seq(
  sourceGenerators in Compile <+= buildInfo,
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    sparkVersion),
  buildInfoPackage := "io.prediction.core")

lazy val conf = file(".") / "conf"

lazy val root = project in file(".") aggregate(
  common,
  core,
  data,
  tools,
  e2)

lazy val common = (project in file("common")).
  settings(sonatypeSettings: _*).
  settings(unmanagedClasspath in Test += conf)

lazy val core = (project in file("core")).
  dependsOn(data).
  //settings(genjavadocSettings: _*).
  settings(pioBuildInfoSettings: _*).
  settings(sonatypeSettings: _*).
  enablePlugins(SbtTwirl).
  settings(unmanagedClasspath in Test += conf)

lazy val data = (project in file("data")).
  dependsOn(common).
  settings(sonatypeSettings: _*).
  settings(unmanagedClasspath in Test += conf)

lazy val tools = (project in file("tools")).
  dependsOn(core).
  dependsOn(data).
  enablePlugins(SbtTwirl).
  settings(unmanagedClasspath in Test += conf)

lazy val e2 = (project in file("e2")).
  settings(sonatypeSettings: _*).
  settings(unmanagedClasspath in Test += conf)

//scalaJavaUnidocSettings
scalaUnidocSettings

/*
unidocAllSources in (JavaUnidoc, unidoc) := {
  (unidocAllSources in (JavaUnidoc, unidoc)).value
    .map(_.filterNot(_.getName.contains("$")))
    .map(_.filterNot(_.getCanonicalPath.contains("engines")))
}
*/

scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
  "-groups",
  "-skip-packages",
  Seq(
    "akka",
    "breeze",
    "html",
    "io.prediction.annotation",
    "io.prediction.controller.html",
    "io.prediction.controller.java",
    "io.prediction.core",
    "io.prediction.data.api",
    "io.prediction.data.examples",
    "io.prediction.data.storage.elasticsearch",
    "io.prediction.data.storage.examples",
    "io.prediction.data.storage.hbase",
    "io.prediction.data.storage.hdfs",
    "io.prediction.data.storage.localfs",
    "io.prediction.data.storage.mongodb",
    "io.prediction.data.view",
    "io.prediction.engines",
    "io.prediction.workflow",
    "io.prediction.tools",
    "org").mkString(":"),
  "-doc-title",
  "PredictionIO Scaladoc",
  "-doc-version",
  version.value,
  "-doc-root-content",
  "docs/scaladoc/rootdoc.txt")

/*
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
*/

lazy val pioUnidoc = taskKey[Unit]("Builds PredictionIO Scaladoc and Javadoc")

pioUnidoc := {
  (unidoc in Compile).value
  val log = streams.value.log
  log.info("Adding custom styling.")
  IO.append(
    crossTarget.value / "unidoc" / "lib" / "template.css",
    IO.read(baseDirectory.value / "docs" / "scaladoc" / "api-docs.css"))
  IO.append(
    crossTarget.value / "unidoc" / "lib" / "template.js",
    IO.read(baseDirectory.value / "docs" / "scaladoc" / "api-docs.js"))
}

pomExtra in ThisBuild := {
  <url>http://prediction.io</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/PredictionIO/PredictionIO</connection>
    <developerConnection>scm:git:git@github.com:PredictionIO/PredictionIO.git</developerConnection>
    <url>github.com/PredictionIO/PredictionIO</url>
  </scm>
  <developers>
    <developer>
      <id>pio</id>
      <name>The PredictionIO Team</name>
      <url>http://prediction.io</url>
    </developer>
  </developers>
}
