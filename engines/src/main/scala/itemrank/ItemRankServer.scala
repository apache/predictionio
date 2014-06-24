package io.prediction.engines.itemrank

import io.prediction.{ Server, BaseServerParams }

class ItemRankServer extends Server[Feature, Prediction, BaseServerParams] {
  //def init(params: BaseServerParams): Unit = {}

  override def combine(feature: Feature,
    predictions: Seq[Prediction]): Prediction = {
      predictions.head
  }
}
