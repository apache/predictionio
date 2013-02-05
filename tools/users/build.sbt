name := "PredictionIO Users Tool"

version := "0.1"

organization := "io.prediction"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.2-SNAPSHOT",
  "jline" % "jline" % "2.9"
)

resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "http://oss.sonatype.org/content/repositories/releases",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true
