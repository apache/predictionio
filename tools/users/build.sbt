import com.typesafe.sbt.packager.Keys._

name := "users"

scalariformSettings

packageArchetype.java_application

bashScriptExtraDefines += "addJava \"-Dconfig.file=${app_home}/../conf/predictionio.conf -Dio.prediction.base=${app_home}/..\""

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.8",
  "jline" % "jline" % "2.9",
  "org.slf4j" % "slf4j-nop" % "1.6.0"
)
