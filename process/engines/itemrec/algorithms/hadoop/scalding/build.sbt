import AssemblyKeys._ // put this at the top of the file

name := "PredictionIO-Process-ItemRec-Algorithms-Hadoop-Scalding"

packageOptions += Package.ManifestAttributes(java.util.jar.Attributes.Name.MAIN_CLASS -> "com.twitter.scalding.Tool")

version in ThisBuild := "0.5.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.9.2"

parallelExecution in Test := false

// NOTE: need to specify resolvers used by subproj as well
resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

resolvers += "Clojars Repository" at "http://clojars.org/repo"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

assemblySettings

test in assembly := {}

assembleArtifact in packageScala := true

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("jsp-api-2.1-6.1.14.jar", "jsp-2.1-6.1.14.jar",
    "jasper-compiler-5.5.12.jar", "janino-2.5.16.jar", "hadoop-core-1.0.3.jar")
  cp filter { jar => excludes(jar.data.getName)}
}

// Some of these files have duplicates, let's ignore:
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case s if s.endsWith(".class") => MergeStrategy.last
    case s if s.endsWith("project.clj") => MergeStrategy.concat
    case s if s.endsWith(".html") => MergeStrategy.last
    case s if s.endsWith(".properties") => MergeStrategy.last
    case s if s.endsWith(".xml") => MergeStrategy.last
    case x => old(x)
  }
}
