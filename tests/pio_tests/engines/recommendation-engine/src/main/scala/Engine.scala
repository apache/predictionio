package org.template.recommendation

import org.apache.predictionio.controller.IEngineFactory
import org.apache.predictionio.controller.Engine

case class Query(
  user: String,
  num: Int
) extends Serializable

case class PredictedResult(
  itemScores: Array[ItemScore]
) extends Serializable

case class ActualResult(
  ratings: Array[Rating]
) extends Serializable

case class ItemScore(
  item: String,
  score: Double
) extends Serializable

object RecommendationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("als" -> classOf[ALSAlgorithm]),
      classOf[Serving])
  }
}
