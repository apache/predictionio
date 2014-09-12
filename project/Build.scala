import sbt._
import Keys._

object PIOBuild extends Build {
  val sparkVersion = SettingKey[String](
    "spark-version",
    "The version of Apache Spark used for building.")
}
