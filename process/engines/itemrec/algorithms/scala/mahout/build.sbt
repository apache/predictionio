import AssemblyKeys._

name := "PredictionIO-Process-ItemRec-Algorithms-Scala-Mahout"

packageOptions += Package.ManifestAttributes(java.util.jar.Attributes.Name.MAIN_CLASS -> "io.prediction.commons.mahout.itemrec.MahoutJob")

version := "0.3-SNAPSHOT"

scalaVersion := "2.10.0"

parallelExecution in Test := false

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

resolvers += "Clojars Repository" at "http://clojars.org/repo"

assemblySettings

test in assembly := {}
