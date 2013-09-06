import AssemblyKeys._

name := "PredictionIO-Process-ItemRec-Algorithms-Scala-Mahout"

packageOptions += Package.ManifestAttributes(java.util.jar.Attributes.Name.MAIN_CLASS -> "io.prediction.commons.mahout.itemrec.MahoutJob")

version in ThisBuild:= "0.6.1"

scalaVersion in ThisBuild:= "2.10.2"

scalacOptions in ThisBuild ++= Seq("-deprecation")

parallelExecution in Test := false

resolvers in ThisBuild ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "Concurrent Maven Repo" at "http://conjars.org/repo",
  "Clojars Repository" at "http://clojars.org/repo")

assemblySettings

test in assembly := {}

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set(
    "jsp-api-2.1-6.1.14.jar",
    "jsp-2.1-6.1.14.jar",
    "jasper-compiler-5.5.12.jar",
    "janino-2.5.16.jar",
    "minlog-1.2.jar",
    "mockito-all-1.8.5.jar",
    "hadoop-core-1.0.4.jar")
  cp filter { jar => excludes(jar.data.getName)}
}

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case ("org/xmlpull/v1/XmlPullParser.class") => MergeStrategy.rename
    case ("org/xmlpull/v1/XmlPullParserException.class") => MergeStrategy.rename
    case x => old(x)
  }
}
