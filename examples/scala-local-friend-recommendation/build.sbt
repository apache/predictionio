import AssemblyKeys._

assemblySettings

<<<<<<< Updated upstream
name := "examples-friendrecommendation"
=======
name := "example-scala-local-friend-recommendation"
>>>>>>> Stashed changes

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction" %% "core" % "0.8.1-SNAPSHOT" % "provided",
  "io.prediction" %% "data" % "0.8.1-SNAPSHOT" % "provided",
  "org.apache.spark" %% "spark-core" % "1.1.0" % "provided")
