import AssemblyKeys._

assemblySettings

name := "example-java-parallel-helloworld"

organization := "org.sample"

libraryDependencies ++= Seq(
  "io.prediction" %% "core" % "0.8.3-SNAPSHOT" % "provided",
  "org.apache.spark" %% "spark-core" % "1.1.0" % "provided")
