name := "predictionio-scheduler"

version := "0.6.7"

organization := "io.prediction"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % version.value,
  "io.prediction" %% "predictionio-output" % version.value,
  "commons-io" % "commons-io" % "2.4",
  "mysql" % "mysql-connector-java" % "5.1.22",
  "org.clapper" %% "scalasti" % "1.0.0",
  "org.quartz-scheduler" % "quartz" % "2.1.7",
  "org.specs2" %% "specs2" % "1.14" % "test")

javaOptions in Test += "-Dconfig.file=conf/test.conf"

play.Project.playScalaSettings

scalariformSettings
