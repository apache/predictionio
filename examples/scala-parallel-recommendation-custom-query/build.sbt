import AssemblyKeys._

assemblySettings

name := "template-scala-parallel-recommendation-custom-query"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction"    % "client"         % "0.8.3" withSources() withJavadoc(),
  "io.prediction"    %% "core"          % "0.8.6",
  "org.apache.spark" %% "spark-core"    % "1.2.0",
  "org.apache.spark" %% "spark-mllib"   % "1.2.0")
