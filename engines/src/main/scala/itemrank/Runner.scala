package io.prediction.engines.itemrank

import io.prediction.core.{ BaseEngine }
import io.prediction.{ EmptyParams }
import io.prediction.{ DefaultServer, DefaultCleanser }
import io.prediction.workflow.SparkWorkflow
//import io.prediction.workflow.EvaluationWorkflow

import com.github.nscala_time.time.Imports._

object Runner {

  def main(args: Array[String]) {
    val evalParams = new EvalParams(
      appid = 1,
      itypes = None,
      actions = Map(
        "view" -> Some(3),
        "like" -> Some(5),
        "conversion" -> Some(4),
        "rate" -> None
      ),
      conflict = "latest",
      //recommendationTime = 123456,
      seenActions = Some(Set("conversion")),
      //(int years, int months, int weeks, int days, int hours,
      // int minutes, int seconds, int millis)
      hours = 24,//new Period(0, 0, 0, 1, 0, 0, 0, 0),
      trainStart = new DateTime("2014-04-01T00:00:00.000"),
      testStart = new DateTime("2014-04-20T00:00:00.000"),
      testUntil = new DateTime("2014-04-30T00:00:00.000"),
      goal = Set("conversion", "view")
    )

    val knnAlgoParams = new KNNAlgoParams(similarity="cosine", k=10)
    val randomAlgoParams = new RandomAlgoParams
    val serverParams = new EmptyParams

    val paramSet = Seq(
      ("knn", knnAlgoParams),
      ("rand", randomAlgoParams))

    val engine = ItemRankEngine()


    val evaluator = ItemRankEvaluator()

    SparkWorkflow.run(
      "Thor",
      evalParams,
      evalParams, /* validation */
      new EmptyParams /* cleanserParams */,
      paramSet,
      serverParams,
      engine,
      evaluator)

    /*
    // Comment by yipjustin. Use SparkWorkflow instead
    val evalWorkflow1 = EvaluationWorkflow(
      "", evalParams, evalParams,
      null,  // cleanserParams
      knnEngineAlgoParamSet, serverParams,
      knnEngine, evaluator)

    val evalWorkflow2 = EvaluationWorkflow(
      "", evalParams, evalParams,
      null, // cleanserParams
      randEngineAlgoParamSet, serverParams,
      randEngine, evaluator)

    // Comment by yipjustin. All tasks are now persist in database. Hence, using
    // one worker suffice.
    evalWorkflow1.run
    evalWorkflow2.run
    */
  }

}
