package io.prediction.output.itemrec

import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{ App, Algo, OfflineEval }
import com.github.nscala_time.time.Imports._

object ItemRecCFAlgoOutput extends ItemRecAlgoOutput {
  override def output(uid: String, n: Int, itypes: Option[Seq[String]],
    instant: DateTime)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]): Iterator[String] = {
    /** FIXME(someone). Ugly hack for realtime algorithms. **/
    if (algo.infoid == "pio-itemrec-single-featurebased_realtime") {
      ItemRecAlgoRealtimeOutput.output(uid, n, itypes, instant)
    } else {
      ItemRecCFAlgoBatchOutput.output(uid, n, itypes, instant)
    }
  }
}
