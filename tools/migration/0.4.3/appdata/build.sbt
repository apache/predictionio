name := "PredictionIO 0.4 to 0.4.3 appdata Migration"

version := "0.4.3-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.mongodb" %% "casbah" % "2.5.0",
  "com.typesafe" % "config" % "1.0.0"
)

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.4.3-SNAPSHOT"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
