package pio.refactor

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine
import io.prediction.controller._

class VanillaEvaluator
  extends Evaluator[EmptyEvaluationInfo, Query, PredictedResult,
  ActualResult, Int, Int, String] {

  def evaluateUnit(q: Query, p: PredictedResult, a: ActualResult): Int = {
    q.q - p.p
  }

  def evaluateSet(evalInfo: EmptyEvaluationInfo, eus: Seq[Int]): Int = eus.sum

  def evaluateAll(input: Seq[(EmptyEvaluationInfo, Int)]): String = {
    val sum = input.map(_._2).sum
    s"VanillaEvaluator(${input.size}, $sum)"
  }
}
