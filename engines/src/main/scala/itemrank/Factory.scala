package io.prediction.engines.itemrank

import io.prediction.{ EngineFactory, EvaluatorFactory }
import io.prediction.core.{ BaseEngine }
import io.prediction.{ DefaultCleanser, DefaultServer }

object ItemRankEngineFactory extends EngineFactory {
  def get() = {
    new BaseEngine(
      classOf[DefaultCleanser[TrainigData]],
      Map("knn" -> classOf[KNNAlgorithm],
        "rand" -> classOf[RandomAlgorithm]),
      classOf[DefaultServer[Feature, Prediction]]
    )
  }
}

object ItemRankEvaluatorFactory extends EvaluatorFactory {
  def get() = {
    new ItemRankEvaluator
  }
}
