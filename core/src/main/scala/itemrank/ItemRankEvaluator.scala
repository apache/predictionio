package io.prediction.itemrank

import io.prediction.{ Evaluator }
import scala.collection.mutable.ArrayBuffer

class ItemRankEvaluator extends Evaluator[EvalParams, TrainDataParams, EvalDataParams, Feature, Target] {

  val result = new ArrayBuffer[(Feature, Target)]

  override def getParamsSet(params: EvalParams): Seq[(TrainDataParams, EvalDataParams)] = {
    // TODO: for now simply feedthrough the params
    val trainingP = new TrainDataParams(
      appid = params.appid,
      itypes = params.itypes,
      actions = params.actions,
      conflict = params.conflict,
      recommendationTime = params.recommendationTime,
      seenActions = params.seenActions
    )

    val evalP = new EvalDataParams(
      testUsers = params.testUsers,
      testItems = params.testItems,
      goal = params.goal
    )
    Seq((trainingP, evalP))
  }

  override def evaluate(feature: Feature, predicted: Target,
    actual: Target): Unit = {
    // simply store result for now
    result += Tuple2(feature, predicted)
  }

  override def report(): Unit = {
    // simply print result
    result.foreach { r =>
      val (f, t) = r
      println(s"${f}, ${t}")
    }
  }

}
