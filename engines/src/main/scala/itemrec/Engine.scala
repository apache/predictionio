package io.prediction.engines.itemrec

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

object ItemRecEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[EventsDataSource],
      classOf[ItemRecPreparator],
      Map(
        "ncMahoutItemBased" -> classOf[NCItemBasedAlgorithm]),
      classOf[ItemRecServing]
    )
  }
}
