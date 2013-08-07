name := "PredictionIO Process Commons Hadoop Scalding"

organization := "io.prediction"

version := "0.6.0"

scalaVersion := "2.10.0"

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

parallelExecution in Test := false

libraryDependencies += "org.apache.hadoop" % "hadoop-core" % "1.0.4"

libraryDependencies += "com.twitter" %% "scalding-core" % "0.8.6"

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.2",
  "org.joda" % "joda-convert" % "1.3.1"
)

// for compiling mongotap
libraryDependencies ++= Seq(
  "org.mongodb" %% "casbah" % "2.6.2",
  "org.mongodb" % "mongo-hadoop-core" % "1.1.0"
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.14" % "test",
  "com.github.nscala-time" %% "nscala-time" % "0.4.2"
)

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.0"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
