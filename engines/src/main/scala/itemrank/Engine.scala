package io.prediction.engines.itemrank

import io.prediction.api.EngineFactory
import io.prediction.api.Engine

object ItemRankEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[ItemRankDataSource],
      classOf[ItemRankPreparator],
      Map("knn" -> classOf[KNNAlgorithm],
        "rand" -> classOf[RandomAlgorithm],
        "mahout" -> classOf[MahoutItemBasedAlgorithm]),
      classOf[ItemRankServing]
    )
  }
}
