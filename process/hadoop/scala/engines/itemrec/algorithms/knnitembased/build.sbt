name := "PredictionIO Algorithms Hadoop Scala ItemRec KNNItemBased"

version := "0.1"

scalaVersion := "2.9.2"

parallelExecution in Test := false

packageOptions +=
  Package.ManifestAttributes( java.util.jar.Attributes.Name.MAIN_CLASS -> "com.twitter.scalding.Tool" )

libraryDependencies += "org.apache.hadoop" % "hadoop-core" % "1.0.3"

libraryDependencies += "com.twitter" % "scalding_2.9.2" % "0.8.1"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.12.3" % "test",
  "io.prediction" %% "predictionio-commons" % "0.1-SNAPSHOT"
)

resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "http://oss.sonatype.org/content/repositories/releases",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

resolvers += "Clojars Repository" at "http://clojars.org/repo"
