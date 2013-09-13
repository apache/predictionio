name := "PredictionIO Output"

version := "0.6.2"

organization := "io.prediction"

scalaVersion := "2.10.2"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Output API Documentation")

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.2",
  "com.github.nscala-time" %% "nscala-time" % "0.4.2",
  "org.specs2" %% "specs2" % "1.14" % "test"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true