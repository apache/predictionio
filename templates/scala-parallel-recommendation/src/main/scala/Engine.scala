package org.template.recommendation

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

case class Rating(
  val user: String,
  val product: String,
  val rating: Double
)

case class Query(
  val user: String,
  val num: Int
) extends Serializable

case class PredictedResult(
  val productScores: Array[ProductScore]
) extends Serializable

case class ProductScore(
  product: String,
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
