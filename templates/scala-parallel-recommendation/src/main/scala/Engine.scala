package org.template.recommendation

import io.prediction.controller._

object RecommendationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("" -> classOf[ALSAlgorithm]),
      classOf[Serving])
  }
}
