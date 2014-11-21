package io.prediction.examples.friendrecommendation

import io.prediction.controller.Evaluator
import io.prediction.engines.base.EvaluatorOutput

case class Query(
  val uid:Int,
  val iid:Int
)extends Serializable{
  override def toString = "[${uid},${iid}]"
}

case class Prediction(
  val confidenceScore: Double,
  val acceptance : Int
) extends Serializable{
  override def toString = s"[${confidenceScore},${acceptance}]"
}

case class Actual(
  val acceptance : Int
)extends Serializable{
  override def toString = s"[${acceptance}]"
}

class EvaluatorUnit (
  val q: Query,
  val p: Prediction,
  val a: Actual,
  val score: Double
) extends Serializable

class FriendRecEvaluator extends Evaluator[EmptyParam,EmptyParam,Query,Prediction,Actual]{
  override def evaluateUnit(query: Query, prediction: Prediction,actual:Actual):EvaluatorUnit = {
    val score: Double = {
      if(actual.acceptance == 0)
        -prediction.confidenceScore
      else
        prediction.confidenceScore
    }
    new EvaluatorUnit(
      q = query,
      p = prediction,
      a = actual,
      score = score
    )
  }


  override def evaluateSet(s: Seq[EvaluatorUnit]):Double = {
    val sum_value : Double = s.map(x => x.score).sum
    sum_value / s.size
  }

  override def evaluateAll(d: Double): Double = {
    d
  }
}
