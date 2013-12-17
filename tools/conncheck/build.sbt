import com.typesafe.sbt.packager.Keys._

name := "conncheck"

scalariformSettings

packageArchetype.java_application

bashScriptExtraDefines += "addJava \"-Dconfig.file=${app_home}/../conf/predictionio.conf -Dio.prediction.base=${app_home}/..\""

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.0"
