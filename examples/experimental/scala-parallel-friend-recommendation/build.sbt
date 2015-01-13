import AssemblyKeys._

assemblySettings

name := "template-scala-parallel-recommendation-custom-preparator"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.6-SNAPSHOT" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.2.0" % "provided",
  "org.apache.spark" %% "spark-graphx"  % "1.2.0" % "provided")
