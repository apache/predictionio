package io.prediction.engines.itemrank

import io.prediction.{ Evaluator, BaseEvaluationResults }
import scala.collection.mutable.ArrayBuffer

class ItemRankEvaluator 
  extends Evaluator[EvalParams, TrainDataParams, EvalDataParams, 
      Feature, Target, EvalUnit, BaseEvaluationResults] {

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
    actual: Target): EvalUnit = {
    // simply store result for now
    new EvalUnit(feature, predicted)
  }

  override def report(evalUnits: Seq[EvalUnit]): BaseEvaluationResults = {
    // simply print result
    evalUnits.foreach { r =>
      val (f, t) = (r.f, r.p)
      println(s"${f}, ${t}")
    }
    null
  }

}
