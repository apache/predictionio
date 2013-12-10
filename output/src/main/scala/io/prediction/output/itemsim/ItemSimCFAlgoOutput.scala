package io.prediction.output.itemsim

import io.prediction.commons.modeldata.ItemSimScore
import io.prediction.commons.settings.{ App, Algo, OfflineEval }

object ItemSimCFAlgoOutput extends ItemSimAlgoOutput {
  def output(iid: String, n: Int, itypes: Option[Seq[String]], after: Option[ItemSimScore] = None)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]) = {
    /** Batch mode output only for now. */
    ItemSimCFAlgoBatchOutput.output(iid, n, itypes, after)
  }
}
