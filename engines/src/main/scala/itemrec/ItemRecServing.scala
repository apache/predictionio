package io.prediction.engines.itemrec

import io.prediction.controller.LServing
import io.prediction.controller.EmptyParams

// Only return first prediction
class ItemRecServing extends LServing[EmptyParams, Query, Prediction] {
  override def serve(query: Query, predictions: Seq[Prediction]): Prediction = {
    predictions.head
  }
}
