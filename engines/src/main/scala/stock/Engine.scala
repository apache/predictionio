package io.prediction.engines.stock

import io.prediction.core.BaseEngine
import io.prediction.DefaultServer
//import io.prediction.DefaultCleanser
import io.prediction.EngineFactory
import io.prediction.DefaultCleanser

class NoOptCleanser extends DefaultCleanser[TrainingData] {}

object StockEngine extends EngineFactory {
  def apply() = {
    new BaseEngine(
      classOf[NoOptCleanser],
      Map("random" -> classOf[RandomAlgorithm],
        "regression" -> classOf[RegressionAlgorithm]),
      classOf[StockServer])
  }

}
