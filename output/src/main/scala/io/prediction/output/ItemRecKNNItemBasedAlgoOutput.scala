package io.prediction.output

import io.prediction.commons.settings._

object ItemRecKNNItemBasedAlgoOutput extends ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo) = {
    /** Batch mode output only for now. */
    ItemRecKNNItemBasedAlgoBatchOutput.output(uid, n, itypes)
  }
}
