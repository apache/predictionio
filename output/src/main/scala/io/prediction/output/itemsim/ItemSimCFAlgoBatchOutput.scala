package io.prediction.output.itemsim

import io.prediction.commons.Config
import io.prediction.commons.modeldata.ItemSimScore
import io.prediction.commons.settings.{Algo, App, OfflineEval}

object ItemSimCFAlgoBatchOutput {
  private val config = new Config

  def output(iid: String, n: Int, itypes: Option[Seq[String]], after: Option[ItemSimScore] = None)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]) = {
    val itemSimScores = offlineEval map { _ => config.getModeldataTrainingItemSimScores } getOrElse config.getModeldataItemSimScores
    itemSimScores.getTopN(iid, n, itypes, after).toSeq
  }
}
