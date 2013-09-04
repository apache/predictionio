import sbt._
import Keys._

object PredictionIOAlgorithmsHadoopScaldingBuild extends Build {
  lazy val root = Project(
    id = "itemsim-algo-scalding",
    base = file(".")).aggregate(
    algo_itemsimcf,
    algo_mahout,
    algo_randomrank,
    algo_latestrank
  ).dependsOn(
    algo_itemsimcf,
    algo_mahout,
    algo_randomrank,
    algo_latestrank
  )

  lazy val algo_itemsimcf = Project(
    id = "algo-itemsimcf",
    base = file("itemsimcf"))

  lazy val algo_mahout = Project(
    id = "algo-mahout",
    base = file("mahout"))

  lazy val algo_randomrank = Project(
    id = "algo-randomrank",
    base = file("randomrank"))

  lazy val algo_latestrank = Project(
    id = "algo-latestrank",
    base = file("latestrank"))
}
