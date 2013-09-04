name := "PredictionIO Commons"

version := "0.6.0"

organization := "io.prediction"

scalaVersion := "2.10.2"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Commons API Documentation")

libraryDependencies ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "0.4.2",
  "com.twitter" %% "chill" % "0.2.3",
  "com.typesafe" % "config" % "1.0.2",
  "org.mongodb" %% "casbah" % "2.6.2",
  "org.specs2" %% "specs2" % "1.14" % "test"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
