import AssemblyKeys._

assemblySettings

name := "PredictionIO-Process-ItemRec-Evaluations-Scala-TrainingTestSplitTime"

version := "0.6.0"

scalaVersion in ThisBuild := "2.9.2"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.0"
)

libraryDependencies += "com.twitter" % "scalding-args_2.9.2" % "0.8.4"

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

