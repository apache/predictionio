
import AssemblyKeys._

assemblySettings

name := "java-local-tutorial"

organization := "io.prediction"

version := "0.8.1"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.1" % "provided",
  "io.prediction"    %% "engines"       % "0.8.1" % "provided",
  "org.apache.mahout" % "mahout-core"   % "0.9",
  "org.apache.spark" %% "spark-core"    % "1.1.0" % "provided")
