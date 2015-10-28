package io.prediction.examples.experimental.cleanupapp

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine
import io.prediction.controller._

case class Query(q: String) extends Serializable

case class PredictedResult(p: String) extends Serializable

object VanillaEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      PIdentityPreparator(classOf[DataSource]),
      Map("" -> classOf[Algorithm]),
      classOf[Serving])
  }
}
