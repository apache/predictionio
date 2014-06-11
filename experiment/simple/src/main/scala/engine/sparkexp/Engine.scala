package io.prediction.engines.sparkexp

import io.prediction.core.{ AbstractEngine, BaseEngine }
import io.prediction.EngineFactory
import io.prediction.{ DefaultCleanser, DefaultServer }

object SparkExpEngine extends EngineFactory {
  override def apply(): AbstractEngine = {
    new BaseEngine(
      classOf[DefaultCleanser[TD]],
      Map("sample" -> classOf[SimpleAlgorithm]),
      classOf[DefaultServer[F, P]]
    )
  }
}
