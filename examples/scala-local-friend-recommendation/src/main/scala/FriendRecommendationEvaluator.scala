package io.prediction.examples.friendrecommendation


import io.prediction.controller._
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

class FriendRecommendationEvaluator extends Evaluator[EmptyParams, FriendRecommendationQuery,FriendRecommendationPrediction,Actual, EvaluatorUnit, Double, Double]{
  override def evaluateUnit(query: FriendRecommendationQuery, prediction: FriendRecommendationPrediction,actual:Actual):EvaluatorUnit = {
    val score: Double = {
      if(actual.acceptance == 0)
        -prediction.confidence
      else
        prediction.confidence
    }
    new EvaluatorUnit(
      q = query,
      p = prediction,
      a = actual,
      score = score
    )
  }

  def evaluateAll(input: Seq[(EmptyParams, Double)]): Double = {
    input.head._2
  }
  def evaluateSet(dataParams: EmptyParams,evaluationUnits: Seq[EvaluatorUnit]): Double = 
  {
    val sum_value : Double = evaluationUnits.map(x => x.score).sum
    sum_value / evaluationUnits.size
  }

}
