import AssemblyKeys._

assemblySettings

name := "PredictionIO-Process-ItemRec-Evaluations-ParamGen"

version := "0.4.2"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.4.2",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "ch.qos.logback" % "logback-core" % "1.0.9",
  "com.typesafe" % "config" % "1.0.0",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)
