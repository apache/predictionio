package io.prediction.output.itemrec.knnitembased

import io.prediction.commons.modeldata.{Config, ItemRecScore}
import io.prediction.commons.settings.{Algo, App}

object ItemRecKNNItemBasedAlgoBatchOutput {
  val config = new Config()
  val itemRecScores = config.getItemRecScores()

  def output(uid: String, n: Int, itypes: Option[List[String]], after: Option[ItemRecScore] = None)(implicit app: App, algo: Algo) = {
    itemRecScores.getTopN(uid, n, itypes, after).toList
  }
}
