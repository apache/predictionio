import sbt._
import Keys._

object PIOBuild extends Build {
  val json4sVersion = SettingKey[String](
    "json4s-version",
    "The version of JSON4S used for building.")
  val sparkVersion = SettingKey[String](
    "spark-version",
    "The version of Apache Spark used for building.")
}
