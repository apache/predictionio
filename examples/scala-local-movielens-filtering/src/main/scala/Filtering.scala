package myorg

import io.prediction.controller.LServing
import io.prediction.controller.Params
import io.prediction.engines.itemrec.Prediction
import io.prediction.engines.itemrec.Query
import scala.io.Source

case class TempFilterParams(val filepath: String) extends Params

class TempFilter(val params: TempFilterParams) 
    extends LServing[TempFilterParams, Query, Prediction] {
  override def serve(query: Query, predictions: Seq[Prediction]): Prediction = {
    val disabledIids: Set[String] = Source.fromFile(params.filepath)
      .getLines()
      .toSet

    val prediction = predictions.head
    // prediction.items is a list of (item_id, score)-tuple
    prediction.copy(items = prediction.items.filter(e => !disabledIids(e._1)))
  }
}
