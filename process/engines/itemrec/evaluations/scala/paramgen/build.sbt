import AssemblyKeys._

assemblySettings

name := "PredictionIO-Process-ItemRec-Evaluations-ParamGen"

version := "0.6.4"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.4",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "ch.qos.logback" % "logback-core" % "1.0.9",
  "com.typesafe" % "config" % "1.0.0",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("minlog-1.2.jar")
  cp filter { jar => excludes(jar.data.getName)}
}
