package io.prediction.engines.itemrank

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

object ItemRankEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[EventsDataSource],
      classOf[ItemRankPreparator],
      Map("rand" -> classOf[RandomAlgorithm],
        "mahoutItemBased" -> classOf[mahout.ItemBasedAlgorithm],
        "featurebased" -> classOf[FeatureBasedAlgorithm],
        "ncMahoutItemBased" -> classOf[ncmahout.ItemBasedAlgorithm]),
      classOf[ItemRankServing]
    )
  }
}
