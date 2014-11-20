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
  override def toString = s"${confidenceScore},${acceptance}"
}

case class Actual(
  val acceptance : Int
)extends Serializable{
  override def toString = s"${acceptance}"
}

class FriendRecEvaluator extends Evaluator[EmptyParam,EmptyParam,Query,Prediction,Actual]{
    

override def evaluateUnit(query: Query, prediction: Prediction,actual:Actual):EvaluatorUnit = {
}

override def evaluateSet(Seq[EvaluatorUnit]:s) = {}

override def evaluateAll(Seq[EvaluatorUnit])

}

