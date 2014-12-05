package org.template.recommendation

import io.prediction.controller.LServing
import io.prediction.controller.Params
import scala.io.Source

case class ServingParams(val filepath: String) extends Params

class Serving(val params: ServingParams)
  extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query, predictedResults: Seq[PredictedResult])
  : PredictedResult = {
    val disabledProducts: Set[Int] = Source
      .fromFile(params.filepath)
      .getLines
      .map(_.toInt)
      .toSet

    val productScores = predictedResults.head.productScores
    PredictedResult(productScores.filter(ps => !disabledProducts(ps.product)))
  }
}
