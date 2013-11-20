name := "PredictionIO Settings Initialization"

version := "0.6.4-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-deprecation")

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.4-SNAPSHOT"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)
