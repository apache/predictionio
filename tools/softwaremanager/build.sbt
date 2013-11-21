name := "PredictionIO Software Manager"

version := "0.6.4"

organization := "io.prediction"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-deprecation")

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.4",
  "com.github.scopt" %% "scopt" % "3.1.0",
  "commons-io" % "commons-io" % "2.4",
  "org.slf4j" % "slf4j-nop" % "1.6.0"
)

libraryDependencies += "org.specs2" %% "specs2" % "2.1.1" % "test"

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)
