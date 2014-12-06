package org.template.recommendation

import io.prediction.controller.LServing
import io.prediction.controller.EmptyServingParams

class Serving
  extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query,
    predictedResults: Seq[PredictedResult]): PredictedResult = {
    predictedResults.head
  }
}
