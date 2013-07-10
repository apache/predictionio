name := "PredictionIO Software Manager"

version := "0.4.3-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-deprecation")

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.4.3-SNAPSHOT",
  "com.github.scopt" %% "scopt" % "3.1.0",
  "com.twitter" %% "scalding-args" % "0.8.5",
  "commons-io" % "commons-io" % "2.4",
  "org.slf4j" % "slf4j-nop" % "1.6.0"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)
