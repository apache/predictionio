package io.prediction.engines.itemrank

import io.prediction.api.LServing
import io.prediction.api.EmptyParams

import io.prediction.{ Server, BaseParams }

class ItemRankServing extends LServing[EmptyParams, Query, Prediction] {
  override def serve(query: Query, predictions: Seq[Prediction]): Prediction = {
    predictions.head
  }
}
