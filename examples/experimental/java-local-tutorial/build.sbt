
import AssemblyKeys._

assemblySettings

name := "java-local-tutorial"

organization := "io.prediction"

version := "0.8.4"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.4" % "provided",
  "io.prediction"    %% "engines"       % "0.8.4" % "provided",
  "org.apache.mahout" % "mahout-core"   % "0.9",
  "org.apache.spark" %% "spark-core"    % "1.2.0" % "provided")
