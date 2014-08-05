package io.prediction.examples.itemrank

import io.prediction.controller.LServing
import io.prediction.controller.EmptyParams

class ItemRankServing extends LServing[EmptyParams, Query, Prediction] {
  override def serve(query: Query, predictions: Seq[Prediction]): Prediction = {
    predictions.head
  }
}
