name := "PredictionIO 0.4 to 0.5 appdata Migration"

version := "0.6.3"

organization := "io.prediction"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.3",
  "org.mongodb" %% "casbah" % "2.6.2",
  "org.slf4j" % "slf4j-nop" % "1.6.0"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
