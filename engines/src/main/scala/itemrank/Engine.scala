package io.prediction.engines.itemrank

import io.prediction.core.{ AbstractEngine, BaseEngine }
import io.prediction.EngineFactory
import io.prediction.{ DefaultCleanser, DefaultServer }

object ItemRankEngine extends EngineFactory {
  override def apply(): AbstractEngine = {
    new BaseEngine(
      classOf[DefaultCleanser[TrainingData]],
      Map("knn" -> classOf[KNNAlgorithm],
        "rand" -> classOf[RandomAlgorithm]),
      classOf[DefaultServer[Feature, Prediction]]
    )
  }
}
