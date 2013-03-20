import sbt._
import Keys._

object PredictionIOAlgorithmsScalaMahoutBuild extends Build {
  lazy val root = Project(
    id = "root",
    base = file(".")).aggregate(
    commons,
    algo_thresholduserbased
  ).dependsOn(
    commons,
    algo_thresholduserbased
  )

  lazy val commons = Project(
    id = "commons",
    base = file("commons"))

  lazy val algo_thresholduserbased = Project(
    id = "algo-thresholduserbased",
    base = file("thresholduserbased")) dependsOn commons

}
