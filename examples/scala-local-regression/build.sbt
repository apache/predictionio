import AssemblyKeys._

assemblySettings

name := "example-scala-local-regression"

organization := "io.prediction"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.0-SNAPSHOT" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.0.2" % "provided",
  "org.json4s"       %% "json4s-native" % "3.2.6",
  "org.scalanlp"      % "nak"           % "1.2.1")
