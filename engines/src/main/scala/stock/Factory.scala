package io.prediction.engines.stock

import io.prediction.core.BaseEngine
import io.prediction.core.AbstractEngine
import io.prediction.core.AbstractEvaluator
import io.prediction.DefaultServer
import io.prediction.DefaultCleanser
import io.prediction.{ EngineFactory, EvaluatorFactory }

object StockEngineFactory extends EngineFactory {
  def get(): AbstractEngine = {
    new BaseEngine(
      classOf[DefaultCleanser[TrainingData]],
      Map("random" -> classOf[RandomAlgorithm],
        "regression" -> classOf[RegressionAlgorithm]),
      classOf[StockServer])
  }
}

object StockEvaluatorFactory extends EvaluatorFactory {
  def get(): AbstractEvaluator = {
    new StockEvaluator
  }
}
