
import AssemblyKeys._

assemblySettings

name := "java-local-tutorial"

organization := "io.prediction"

version := "0.8.2"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.2" % "provided",
  "io.prediction"    %% "engines"       % "0.8.2" % "provided",
  "org.apache.mahout" % "mahout-core"   % "0.9",
  "org.apache.spark" %% "spark-core"    % "1.1.0" % "provided")
