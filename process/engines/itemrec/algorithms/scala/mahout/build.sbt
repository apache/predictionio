import AssemblyKeys._

name := "predictionio-process-itemrec-algorithms-scala-mahout"

packageOptions += Package.ManifestAttributes(java.util.jar.Attributes.Name.MAIN_CLASS -> "io.prediction.commons.mahout.itemrec.MahoutJob")

parallelExecution in Test := false

resolvers ++= Seq(
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
