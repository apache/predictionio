name := "PredictionIO Commons"

version := "0.1-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.9.2"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Commons API Documentation")

crossScalaVersions := Seq("2.9.1", "2.9.2")

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.0.0",
  "commons-codec" % "commons-codec" % "1.7",
  "org.mongodb" %% "casbah" % "2.5.0",
  "org.scalaj" %% "scalaj-time" % "0.6",
  "org.specs2" %% "specs2" % "1.12.3" % "test"
)

resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "http://oss.sonatype.org/content/repositories/releases"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
