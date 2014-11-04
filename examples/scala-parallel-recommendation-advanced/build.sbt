import AssemblyKeys._

assemblySettings

name := "examples-scala-parallel-recommendation-advanced"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.1" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.1.0" % "provided",
  "org.apache.spark" %% "spark-mllib"   % "1.1.0" % "provided")

// the following is needed for reading from Mongo to spark
libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-hadoop-core" % "1.3.0"
    exclude("org.apache.hadoop", "hadoop-yarn-api")
    exclude("org.apache.hadoop", "hadoop-common"))
