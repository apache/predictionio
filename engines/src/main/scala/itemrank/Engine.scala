package io.prediction.engines.itemrank

import io.prediction.api.IEngineFactory
import io.prediction.api.Engine

object ItemRankEngine extends IEngineFactory {
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
