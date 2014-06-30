package io.prediction.engines.itemrank

import io.prediction.core.BaseEngine
import io.prediction.EngineFactory
import io.prediction.{ DefaultCleanser, DefaultServer }

class NoOptCleanser extends DefaultCleanser[TrainingData] {}

object ItemRankEngine extends EngineFactory {
  def apply() = {
    new BaseEngine(
      classOf[ItemRankCleanser],
      Map("knn" -> classOf[KNNAlgorithm],
        "rand" -> classOf[RandomAlgorithm]),
      classOf[DefaultServer[Feature, Prediction]]
    )
  }
}
