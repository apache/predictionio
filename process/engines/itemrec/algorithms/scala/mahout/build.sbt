import AssemblyKeys._

name := "PredictionIO-Process-ItemRec-Algorithms-Scala-Mahout"

packageOptions += Package.ManifestAttributes(java.util.jar.Attributes.Name.MAIN_CLASS -> "io.prediction.commons.mahout.itemrec.MahoutJob")

version in ThisBuild:= "0.3-SNAPSHOT"

scalaVersion in ThisBuild:= "2.10.0"

parallelExecution in Test := false

resolvers in ThisBuild ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "Concurrent Maven Repo" at "http://conjars.org/repo",
  "Clojars Repository" at "http://clojars.org/repo")

assemblySettings

test in assembly := {}
