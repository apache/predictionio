import sbt._
import Keys._

object PredictionIOEvaluationsHadoopScaldingBuild extends Build {
  lazy val root = Project(
    id = "itemrec-eval-scalding",
    base = file(".")).aggregate(
    eval_metric_map,
    eval_trainingtestsplit
  ).dependsOn(
    eval_metric_map,
    eval_trainingtestsplit
  )

  lazy val eval_trainingtestsplit = Project(
    id = "eval-trainingtestsplit",
    base = file("trainingtestsplit"))

  lazy val eval_metric_map = Project(
    id = "eval-metric-map",
    base = file("metrics/map"))

}
