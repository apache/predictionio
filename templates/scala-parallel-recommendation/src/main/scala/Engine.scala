package org.template.recommendation

import io.prediction.controller._

case class Query(
  val user: Int,
  val num: Int
) extends Serializable

case class ProductScore(
  product: Int,
  score: Double
) extends Serializable

case class Prediction(
  val productScores: Array[ProductScore]
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
