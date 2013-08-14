import sbt._
import Keys._

object PredictionIOAlgorithmsHadoopScaldingBuild extends Build {
  lazy val root = Project(
    id = "itemsim-algo-scalding",
    base = file(".")).aggregate(
    algo_itemsimcf,
    algo_mahout
  ).dependsOn(
    algo_itemsimcf,
    algo_mahout
  )

  lazy val algo_itemsimcf = Project(
    id = "algo-itemsimcf",
    base = file("itemsimcf"))

  lazy val algo_mahout = Project(
    id = "algo-mahout",
    base = file("mahout"))
}
