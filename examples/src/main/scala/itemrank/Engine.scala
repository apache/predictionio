package io.prediction.examples.itemrank

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

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
