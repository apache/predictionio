import scalariform.formatter.preferences._

name := "pio"

version in ThisBuild := "0.8.0-SNAPSHOT"

organization in ThisBuild := "io.prediction"

scalaVersion in ThisBuild := "2.11.0"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions in (ThisBuild, Test) ++= Seq("-Yrangepos")

javacOptions in ThisBuild ++= Seq("-source", "1.6", "-target", "1.6",
  "-Xlint:deprecation", "-Xlint:unchecked")

def sharedSettings = scalariformSettings ++ Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true))

lazy val root = project in file(".") aggregate(
  core)

lazy val core = project in file("core") settings(sharedSettings: _*)
