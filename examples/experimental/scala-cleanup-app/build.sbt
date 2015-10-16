import AssemblyKeys._

assemblySettings

name := "template-scala-parallel-vanilla"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.9.5" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.3.1" % "provided",
  "org.apache.spark" %% "spark-mllib"   % "1.3.1" % "provided")
