package org.template.recommendation

import io.prediction.controller._

class Query(
  val user: Int,
  val product: Int
) extends Serializable

class Prediction(
  val rating: Double
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
