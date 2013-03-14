import sbt._
import Keys._

object PredictionIOAlgorithmsHadoopScaldingBuild extends Build {
  lazy val root = Project(
    id = "itemsim-algo-scalding",
    base = file(".")).aggregate(
    algo_itemsimcf
  ).dependsOn(
    algo_itemsimcf
  )

  lazy val algo_itemsimcf = Project(
    id = "algo-itemsimcf",
    base = file("itemsimcf"))

}
