package io.prediction.output

import io.prediction.commons.settings._

object ItemSimCFAlgoOutput extends ItemSimAlgoOutput {
  def output(iid: String, itypes: Option[List[String]])(implicit algo: Algo) = {
    /** Batch mode output only for now. */
    ItemSimCFAlgoBatchOutput.output(iid, itypes)
  }
}
