package io.prediction.engines.itemrank

import io.prediction.{ Server, BaseParams }

// Note from yipjustin: You can simply put
// classOf[Server[Feature, Prediction, BaseServerParams]] in the EngineFactory.
class ItemRankServer extends Server[Feature, Prediction, BaseParams] {
  override def combine(feature: Feature,
    predictions: Seq[Prediction]): Prediction = {
      predictions.head
  }
}
