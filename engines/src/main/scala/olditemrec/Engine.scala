package io.prediction.engines.olditemrec

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

import io.prediction.engines.itemrank.EventsDataSource

//import io.prediction.engines.olditemrec.NewItemRecDataSource
//import io.prediction.engines.olditemrec.NewItemRecPreparator
import io.prediction.engines.java.olditemrec.algos.GenericItemBased
import io.prediction.engines.java.olditemrec.algos.SVDPlusPlus
import io.prediction.engines.java.olditemrec.ItemRecServing


object ItemRecEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[NewItemRecDataSource],
      classOf[NewItemRecPreparator],
      Map(
        "genericitembased" -> classOf[GenericItemBased],
        "svdplusplus" -> classOf[SVDPlusPlus]),
      classOf[ItemRecServing])
  }
}
