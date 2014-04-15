package io.prediction.output.itemrec

import io.prediction.commons.Config
import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{ Algo, App, OfflineEval }

object ItemRecCFAlgoBatchOutput {
  private val config = new Config

  def output(uid: String, n: Int,
    itypes: Option[Seq[String]])(implicit app: App, algo: Algo,
      offlineEval: Option[OfflineEval]): Seq[(String, Double)] = {
    val itemRecScores = offlineEval map { _ =>
      config.getModeldataTrainingItemRecScores
    } getOrElse config.getModeldataItemRecScores
    itemRecScores.getTopNIidsAndScores(uid, n, itypes)
  }
}
