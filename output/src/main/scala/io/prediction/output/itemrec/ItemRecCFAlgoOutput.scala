package io.prediction.output.itemrec

import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{ App, Algo, OfflineEval }

object ItemRecCFAlgoOutput extends ItemRecAlgoOutput {
  override def output(uid: String, n: Int,
    itypes: Option[Seq[String]])(implicit app: App, algo: Algo,
      offlineEval: Option[OfflineEval]): Seq[(String, Double)] = {
    /** Batch mode output only for now. */
    ItemRecCFAlgoBatchOutput.output(uid, n, itypes)
  }
}
