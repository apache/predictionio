name := "PredictionIO Commons Hadoop Scala"

version := "0.1"

scalaVersion := "2.9.2"

parallelExecution in Test := false

libraryDependencies += "org.apache.hadoop" % "hadoop-core" % "1.0.3"

libraryDependencies += "com.twitter" % "scalding_2.9.2" % "0.8.1"

// for compiling mongotap
libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-hadoop-core_cdh3u3" % "1.0.0-rc0",
  "org.mongodb" % "mongo-hadoop-streaming" % "1.1.0-SNAPSHOT",
  "org.mongodb" %% "casbah" % "2.5.0"
)
//"com.mongodb.casbah" % "casbah_2.9.1" % "2.1.5-1"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.12.3" % "test",
  "com.github.nscala-time" %% "nscala-time" % "0.2.0",
  "io.prediction" %% "predictionio-commons" % "0.2-SNAPSHOT"
)

resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "http://oss.sonatype.org/content/repositories/releases",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

resolvers += "Clojars Repository" at "http://clojars.org/repo"
