import AssemblyKeys._

assemblySettings

name := "predictionio-process-commons-evaluations-scala-trainingtestsplittime"

libraryDependencies += "com.twitter" %% "scalding-args" % "0.8.6"

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("minlog-1.2.jar")
  cp filter { jar => excludes(jar.data.getName)}
}
