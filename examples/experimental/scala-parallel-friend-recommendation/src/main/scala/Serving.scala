package io.prediction.examples.pfriendrecommendation

import io.prediction.controller.LServing
import io.prediction.controller.EmptyServingParams

class Serving
  extends LServing[Query, Double] {

  override
  def serve(query: Query, predictedResults: Seq[Double]): Double = {
    predictedResults.head
  }
}
