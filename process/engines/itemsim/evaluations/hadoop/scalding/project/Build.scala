import sbt._
import Keys._

object PredictionIOEvaluationsHadoopScaldingBuild extends Build {
  lazy val root = Project(
    id = "itemsim-eval-scalding",
    base = file(".")).aggregate(eval_metric_map).dependsOn(eval_metric_map)

  lazy val eval_metric_map = Project(
    id = "eval-metric-map",
    base = file("metrics/ismap"))
}
