package io.prediction.output.itemrec.knnitembased

import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{App, Algo}
import io.prediction.output.itemrec.ItemRecAlgoOutput

object ItemRecKNNItemBasedAlgoOutput extends ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]], after: Option[ItemRecScore] = None)(implicit app: App, algo: Algo) = {
    /** Batch mode output only for now. */
    ItemRecKNNItemBasedAlgoBatchOutput.output(uid, n, itypes, after)
  }
}
