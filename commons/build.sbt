name := "PredictionIO Commons"

version := "0.4.3-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.10.0"

crossScalaVersions := Seq("2.9.2", "2.10.0")

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Commons API Documentation")

libraryDependencies ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "0.2.0",
  "com.twitter" %% "chill" % "0.2.3",
  "com.typesafe" % "config" % "1.0.0",
  "commons-codec" % "commons-codec" % "1.7",
  "org.mongodb" %% "casbah" % "2.5.0",
  "org.specs2" %% "specs2" % "1.12.3" % "test"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
