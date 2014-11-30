package io.prediction.examples.friendrecommendation


import io.prediction.controller.Evaluator
case class Actual(
  val acceptance : Int
)extends Serializable{
  override def toString = s"[${acceptance}]"
}

class EvaluatorUnit (
  val q: FriendRecommendationQuery,
  val p: FriendRecommendationPrediction,
  val a: Actual,
  val score: Double
) extends Serializable

class FriendRecEvaluator extends Evaluator[EmptyParam,FriendRecommendationQuery,FriendRecommendationPrediction,Actual, EvaluatorUnit, Double, Double]{
  override def evaluateUnit(query: FriendRecommendationQuery, prediction: FriendRecommendationPrediction,actual:Actual):EvaluatorUnit = {
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
