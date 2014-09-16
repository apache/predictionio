package io.prediction.engines.itemrec

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

import io.prediction.engines.itemrank.EventsDataSource

import io.prediction.engines.itemrec.NewItemRecDataSource
import io.prediction.engines.itemrec.NewItemRecPreparator
import io.prediction.engines.java.itemrec.algos.GenericItemBased
import io.prediction.engines.java.itemrec.algos.SVDPlusPlus
import io.prediction.engines.java.itemrec.ItemRecServing


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
