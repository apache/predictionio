name := "PredictionIO Connection Check Tool"

version := "0.6.3"

organization := "io.prediction"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.3",
  "org.slf4j" % "slf4j-nop" % "1.6.0"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)
