package myorg

import io.prediction.controller.Engine
import io.prediction.controller.IEngineFactory
import io.prediction.engines.itemrec.EventsDataSource
import io.prediction.engines.itemrec.ItemRecPreparator
import io.prediction.engines.itemrec.NCItemBasedAlgorithm

object TempFilterEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[EventsDataSource],
      classOf[ItemRecPreparator],
      Map("ncMahoutItemBased" -> classOf[NCItemBasedAlgorithm]),
      classOf[TempFilter]
    )
  }
}
