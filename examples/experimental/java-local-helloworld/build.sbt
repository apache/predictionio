import AssemblyKeys._

assemblySettings

name := "example-java-local-helloworld"

organization := "org.sample"

libraryDependencies ++= Seq(
  "io.prediction" %% "core" % "0.9.0" % "provided",
  "org.apache.spark" %% "spark-core" % "1.2.0" % "provided")
