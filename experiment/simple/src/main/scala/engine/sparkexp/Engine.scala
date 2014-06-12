package io.prediction.engines.sparkexp

import io.prediction.core.BaseEngine
import io.prediction.EngineFactory
import io.prediction.{ DefaultCleanser, DefaultServer }

object SparkExpEngine extends EngineFactory {
  override def apply(): BaseEngine[TD, TD, F, P] = {
    new BaseEngine(
      classOf[DefaultCleanser[TD]],
      Map("sample" -> classOf[SimpleAlgorithm]),
      classOf[DefaultServer[F, P]]
    )
  }
}
