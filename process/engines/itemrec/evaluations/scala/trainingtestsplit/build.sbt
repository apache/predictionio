import AssemblyKeys._

assemblySettings

name := "PredictionIO-Process-ItemRec-Evaluations-Scala-TrainingTestSplitTime"

version := "0.6.1"

scalaVersion in ThisBuild := "2.10.2"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.1"
)

libraryDependencies += "com.twitter" %% "scalding-args" % "0.8.6"

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("minlog-1.2.jar")
  cp filter { jar => excludes(jar.data.getName)}
}
