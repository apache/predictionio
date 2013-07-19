name := "PredictionIO Process Commons Hadoop Scalding"

organization := "io.prediction"

version := "0.4.3"

scalaVersion := "2.9.2"

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

parallelExecution in Test := false

libraryDependencies += "org.apache.hadoop" % "hadoop-core" % "1.0.3"

libraryDependencies += "com.twitter" %% "scalding" % "0.8.1"

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.1",
  "org.joda" % "joda-convert" % "1.2"
)

// for compiling mongotap
libraryDependencies ++= Seq(
  "org.mongodb" %% "casbah" % "2.6.2",
  "org.mongodb" % "mongo-hadoop-core" % "1.1.0"
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.12.3" % "test",
  "com.github.nscala-time" %% "nscala-time" % "0.2.0"
)

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.4.3"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
