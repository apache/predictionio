name := "PredictionIO 0.3 to 0.4 Settings Migration"

version := "0.4"

organization := "io.prediction"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.mongodb" %% "casbah" % "2.5.0",
  "com.typesafe" % "config" % "1.0.0"
)
