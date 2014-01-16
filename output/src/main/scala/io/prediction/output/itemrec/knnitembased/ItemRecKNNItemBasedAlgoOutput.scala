package io.prediction.output.itemrec.knnitembased

import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{ App, Algo, OfflineEval }
import io.prediction.output.itemrec.ItemRecAlgoOutput

object ItemRecKNNItemBasedAlgoOutput extends ItemRecAlgoOutput {
  override def output(uid: String, n: Int, itypes: Option[Seq[String]])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]): Iterator[String] = {
    /** Batch mode output only for now. */
    ItemRecKNNItemBasedAlgoBatchOutput.output(uid, n, itypes)
  }
}
