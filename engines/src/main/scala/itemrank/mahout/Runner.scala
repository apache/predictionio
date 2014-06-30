package io.prediction.engines.itemrank

import io.prediction.core.{ BaseEngine }
import io.prediction.{ EmptyParams }
import io.prediction.{ DefaultServer, DefaultCleanser }
import io.prediction.workflow.SparkWorkflow
//import io.prediction.workflow.EvaluationWorkflow

import com.github.nscala_time.time.Imports._

object MahoutRunner {

  def main(args: Array[String]) {
    val evalParams = new EvalParams(
      appid = 1,
      itypes = None,
      actions = Set("view", "like", "conversion", "rate"),
      //(int years, int months, int weeks, int days, int hours,
      // int minutes, int seconds, int millis)
      // number of hours of each period
      hours = 24,//new Period(0, 0, 0, 1, 0, 0, 0, 0),
      trainStart = new DateTime("2014-04-01T00:00:00.000"),
      testStart = new DateTime("2014-04-20T00:00:00.000"),
      testUntil = new DateTime("2014-04-30T00:00:00.000"),
      goal = Set("conversion", "view")
    )

    val cleanserParams = new CleanserParams(
      actions = Map(
        "view" -> Some(3),
        "like" -> Some(5),
        "conversion" -> Some(4),
        "rate" -> None
      ),
      conflict = "latest"
    )

    val mahoutAlgoParams = new MahoutItemBasedAlgoParams(
      booleanData = true,
      itemSimilarity = "LogLikelihoodSimilarity",
      weighted = false,
      nearestN = 10,
      threshold = 5e-324,
      numSimilarItems = 50
    )
    val serverParams = new EmptyParams

    val paramSet = Seq(("mahout", mahoutAlgoParams))

    val engine = new BaseEngine(
      classOf[ItemRankCleanser],
      Map("mahout" -> classOf[MahoutItemBasedAlgorithm]),
      classOf[DefaultServer[Feature, Prediction]]
    )

    val evaluator = ItemRankEvaluator()

    SparkWorkflow.run(
      "Thor",
      evalParams,
      evalParams, /* validation */
      cleanserParams,
      paramSet,
      serverParams,
      engine,
      evaluator)

  }

}
