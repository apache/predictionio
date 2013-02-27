package io.prediction.output.itemrec.knnitembased

import io.prediction.commons.modeldata.{Config, ItemRecScore, TrainingSetConfig}
import io.prediction.commons.settings.{Algo, App, OfflineEval}

object ItemRecKNNItemBasedAlgoBatchOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]], after: Option[ItemRecScore] = None)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]) = {
    val itemRecScores = offlineEval map { _ => (new TrainingSetConfig).getItemRecScores } getOrElse (new Config).getItemRecScores
    itemRecScores.getTopN(uid, n, itypes, after).toList
  }
}
