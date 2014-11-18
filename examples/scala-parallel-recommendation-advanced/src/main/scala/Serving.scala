package org.examples.recommendation

import io.prediction.controller._

class Serving
  extends Serving[EmptyServingParams, Query, PredictedResult] {

  override
  def serve(query: Query,
    predictions: Seq[PredictedResult]): PredictedResult = {
    predictions.head
  }
}
