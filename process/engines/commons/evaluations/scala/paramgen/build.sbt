import AssemblyKeys._

assemblySettings

name := "predictionio-process-commons-evaluations-paramgen"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "ch.qos.logback" % "logback-core" % "1.0.9",
  "com.typesafe" % "config" % "1.0.0",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1")

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("minlog-1.2.jar")
  cp filter { jar => excludes(jar.data.getName)}
}
