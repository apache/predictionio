package io.prediction.engines.itemrank

import io.prediction.api.LServing
import io.prediction.api.EmptyParams

class ItemRankServing extends LServing[EmptyParams, Query, Prediction] {
  override def serve(query: Query, predictions: Seq[Prediction]): Prediction = {
    predictions.head
  }
}
