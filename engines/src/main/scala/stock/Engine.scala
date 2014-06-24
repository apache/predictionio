package io.prediction.engines.stock

import io.prediction.core.BaseEngine
import io.prediction.DefaultServer
//import io.prediction.DefaultCleanser
import io.prediction.EngineFactory
import io.prediction.DefaultCleanser
//import io.prediction.BaseDefaultCleanser

import org.apache.spark.rdd.RDD 

class NoOptCleanser extends DefaultCleanser[TrainingData] {}
//class NoOptCleanser extends BaseDefaultCleanser[TrainingData] {}

object StockEngine extends EngineFactory {
  def apply() = {
    new BaseEngine(
      //classOf[BaseDefaultCleanser[RDD[TrainingData]]],
      classOf[NoOptCleanser],
      Map("random" -> classOf[RandomAlgorithm],
        "regression" -> classOf[RegressionAlgorithm]),
      classOf[StockServer])
  }

}
