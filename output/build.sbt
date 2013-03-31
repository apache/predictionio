name := "PredictionIO Output"

version := "0.3"

organization := "io.prediction"

scalaVersion := "2.10.0"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Output API Documentation")

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.3",
  "com.github.nscala-time" %% "nscala-time" % "0.2.0",
  "junit" % "junit" % "4.11",
  "org.specs2" %% "specs2" % "1.14" % "test"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

testOptions in Test += Tests.Argument("junitxml")

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
