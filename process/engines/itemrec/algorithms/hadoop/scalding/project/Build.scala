import sbt._
import Keys._

object PredictionIOAlgorithmsHadoopScaldingBuild extends Build {
  lazy val root = Project(
    id = "itemrec-algo-scalding",
    base = file(".")).aggregate(
    algo_generic,
    algo_knnitembased,
    algo_randomrank,
    algo_latestrank,
    algo_mahout
  ).dependsOn(
    algo_generic,
    algo_knnitembased,
    algo_randomrank,
    algo_latestrank,
    algo_mahout
  )

  lazy val algo_generic = Project(
    id = "algo-generic",
    base = file("generic"))

  lazy val algo_knnitembased = Project(
    id = "algo-knnitembased",
    base = file("knnitembased"))

  lazy val algo_randomrank = Project(
    id = "algo-randomrank",
    base = file("randomrank"))

  lazy val algo_latestrank = Project(
    id = "algo-latestrank",
    base = file("latestrank"))

  lazy val algo_mahout = Project(
    id = "algo-mahout",
    base = file("mahout"))

}
