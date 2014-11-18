package org.template.recommendation

import io.prediction.controller.LServing
import io.prediction.controller.EmptyServingParams

class Serving
  extends LServing[EmptyServingParams, Query, PredictedResult] {

  override
  def serve(query: Query,
    predictions: Seq[PredictedResult]): PredictedResult = {
    predictions.head
  }
}
