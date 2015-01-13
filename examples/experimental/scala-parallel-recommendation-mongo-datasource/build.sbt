import AssemblyKeys._

assemblySettings

name := "template-scala-parallel-recommendation"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.6-SNAPSHOT" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.2.0" % "provided",
  "org.apache.spark" %% "spark-mllib"   % "1.2.0" % "provided")

// ADDED FOR READING FROM MONGO IN DATASOURCE
libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-hadoop-core" % "1.3.0"
    exclude("org.apache.hadoop", "hadoop-yarn-api")
    exclude("org.apache.hadoop", "hadoop-common"))
