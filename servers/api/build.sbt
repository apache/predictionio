name := "predictionio-api"

version := "0.7.3-SNAPSHOT"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % version.value,
  "io.prediction" %% "predictionio-output" % version.value)

javaOptions in Test += "-Dconfig.file=conf/test.conf"

play.Project.playScalaSettings

scalariformSettings
