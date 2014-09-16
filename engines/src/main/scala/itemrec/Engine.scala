package io.prediction.engines.itemrec

import io.prediction.controller.IEngineFactor
import io.prediction.controller.Engine

import io.prediction.engines.itemrank.EventsDataSource

object ItemRecEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[EventsDataSource]
    )
  }
}
