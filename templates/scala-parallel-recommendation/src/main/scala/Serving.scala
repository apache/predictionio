package org.template.recommendation

import io.prediction.controller._

class Serving
  extends LServing[EmptyServingParams, Query, PredictedResult] {

  override
  def serve(query: Query,
    predictions: Seq[PredictedResult]): PredictedResult = {
    predictions.head
  }
}
