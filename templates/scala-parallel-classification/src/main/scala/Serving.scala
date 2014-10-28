package org.template.classification

import io.prediction.controller._

class Serving
  extends LServing[EmptyServingParams, Query, Prediction] {

  override
  def serve(query: Query, predictions: Seq[Prediction]): Prediction = {
    predictions.head
  }
}
