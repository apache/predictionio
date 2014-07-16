name := "pio"

version in ThisBuild := "0.8.0-SNAPSHOT"

organization in ThisBuild := "io.prediction"

scalaVersion in ThisBuild := "2.10.4"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions in (ThisBuild, Test) ++= Seq("-Yrangepos")

fork in (ThisBuild, run) := true

javacOptions in ThisBuild ++= Seq("-source", "1.7", "-target", "1.7",
  "-Xlint:deprecation", "-Xlint:unchecked")


lazy val root = project in file(".") aggregate(
  core,
  //deploy,
  engines)

lazy val core = (project in file("core"))

lazy val deploy = project in file("deploy") dependsOn(core)

lazy val engines = (project in file("engines")).
  dependsOn(core)

//lazy val experiment = project in file("experiment/simple") dependsOn(core)
