name := "PredictionIO Output"

version := "0.1-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.9.2"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Output API Documentation")

crossScalaVersions := Seq("2.9.1", "2.9.2")

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.1-SNAPSHOT",
  "org.specs2" %% "specs2" % "1.12.3" % "test"
)

resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "http://oss.sonatype.org/content/repositories/releases",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
