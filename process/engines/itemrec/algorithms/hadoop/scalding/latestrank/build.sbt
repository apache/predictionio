name := "PredictionIO Process ItemRec Algorithms Hadoop Scalding LatestRank"

version := "0.5.0-SNAPSHOT"

scalaVersion := "2.9.2"

parallelExecution in Test := false

packageOptions +=
  Package.ManifestAttributes( java.util.jar.Attributes.Name.MAIN_CLASS -> "com.twitter.scalding.Tool" )

libraryDependencies += "org.apache.hadoop" % "hadoop-core" % "1.0.3"

libraryDependencies += "com.twitter" % "scalding_2.9.2" % "0.8.3"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.12.3" % "test"
)

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.5.0-SNAPSHOT",
  "io.prediction" %% "predictionio-process-commons-hadoop-scalding" % "0.5.0-SNAPSHOT"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

resolvers += "Clojars Repository" at "http://clojars.org/repo"
