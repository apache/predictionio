import play.Project._

name := "predictionio"

version := "0.7.0-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "0.4.2")

lazy val root = project.in(file("."))
  .aggregate(commons, output, admin, api, scheduler)

lazy val commons = project in file("commons")

lazy val output = project.in(file("output"))
  .dependsOn(commons)

lazy val admin = project.in(file("servers/admin"))
	.dependsOn(commons, output)

lazy val api = project.in(file("servers/api"))
	.dependsOn(commons, output)

lazy val scheduler = project.in(file("servers/scheduler"))
	.dependsOn(commons)
