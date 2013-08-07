import AssemblyKeys._

assemblySettings

name := "PredictionIO-Process-ItemRec-Evaluations-Scala-TrainingTestSplitTime"

version := "0.6.0"

scalaVersion in ThisBuild := "2.10.0"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.0"
)

libraryDependencies += "com.twitter" %% "scalding-args" % "0.8.6"

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

