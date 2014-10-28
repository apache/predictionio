package org.template.classification

import io.prediction.controller._

object ClassificationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("" -> classOf[NaiveBayesAlgorithm]),
      classOf[Serving])
  }
}
