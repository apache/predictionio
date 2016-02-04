package org.template.classification

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

case class Query(
  features: Array[Double]
)

case class PredictedResult(
  label: Double
)

object ClassificationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("naive" -> classOf[NaiveBayesAlgorithm],
        "randomforest" -> classOf[RandomForestAlgorithm]), // ADDED
      classOf[Serving])
  }
}
