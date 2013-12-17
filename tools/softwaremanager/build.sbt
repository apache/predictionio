import com.typesafe.sbt.packager.Keys._

name := "softwaremanager"

scalariformSettings

packageArchetype.java_application

bashScriptExtraDefines += "addJava \"-Dconfig.file=${app_home}/../conf/predictionio.conf -Dio.prediction.base=${app_home}/..\""

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.1.0",
  "commons-io" % "commons-io" % "2.4",
  "org.slf4j" % "slf4j-nop" % "1.6.0")
