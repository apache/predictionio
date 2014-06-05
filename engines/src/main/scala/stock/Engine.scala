package io.prediction.engines.stock

import io.prediction.core.BaseEngine
import io.prediction.core.AbstractEngine
import io.prediction.DefaultServer
import io.prediction.DefaultCleanser
import io.prediction.EngineFactory

object StockEngine extends EngineFactory {
  override def apply(): BaseEngine[TrainingData,TrainingData,Feature,Target] = {
    new BaseEngine(
      classOf[DefaultCleanser[TrainingData]],
      Map("random" -> classOf[RandomAlgorithm],
        "regression" -> classOf[RegressionAlgorithm]),
      classOf[StockServer])
  }

}
