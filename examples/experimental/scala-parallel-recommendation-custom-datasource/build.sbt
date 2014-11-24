import AssemblyKeys._

assemblySettings

name := "template-scala-parallel-recommendation"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.3-SNAPSHOT" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.1.0" % "provided",
  "org.apache.spark" %% "spark-mllib"   % "1.1.0" % "provided")
