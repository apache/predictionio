import AssemblyKeys._

assemblySettings

name := "examples-friendrecommendation"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction" %% "core" % "0.8.6" % "provided",
  "io.prediction" %% "data" % "0.8.6" % "provided",
  "org.apache.spark" %% "spark-core" % "1.2.0" % "provided")
