name := "PredictionIO Users Tool"

version := "0.6.0-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.6.0-SNAPSHOT",
  "commons-codec" % "commons-codec" % "1.8",
  "jline" % "jline" % "2.9"
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
