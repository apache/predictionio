package io.prediction.engines.itemrank

import io.prediction.{ Evaluator, BaseEvaluationResults }
import scala.collection.mutable.ArrayBuffer
import com.github.nscala_time.time.Imports._

class ItemRankEvaluator
  extends Evaluator[EvalParams, TrainDataPrepParams, EvalDataPrepParams,
      Feature, Target, Target, EvalUnit, BaseEvaluationResults] {

  override def getParamsSet(params: EvalParams): Seq[(TrainDataPrepParams,
    EvalDataPrepParams)] = {

    var testStart = params.testStart
    val testStartSeq = ArrayBuffer[DateTime]()
    while (testStart < params.testUntil) {
      testStartSeq += testStart
      testStart = testStart + params.period
    }

    val paramSeq = testStartSeq.toList.map { ts =>
      val trainingP = new TrainDataPrepParams(
        appid = params.appid,
        itypes = params.itypes,
        actions = params.actions,
        conflict = params.conflict,
        seenActions = params.seenActions,
        startUntil = Some((params.trainStart, ts))
      )
      val evalP = new EvalDataPrepParams(
        appid = params.appid,
        itypes = params.itypes,
        startUntil = (ts, ts + params.period),
        goal = params.goal
      )
      (trainingP, evalP)
    }
    paramSeq
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
