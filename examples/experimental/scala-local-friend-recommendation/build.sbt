import AssemblyKeys._

assemblySettings

name := "examples-friendrecommendation"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction" %% "core" % "0.9.1" % "provided",
  "io.prediction" %% "data" % "0.9.1" % "provided",
  "org.apache.spark" %% "spark-core" % "1.2.0" % "provided")
