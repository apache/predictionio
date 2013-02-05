package io.prediction.output.itemrec.knnitembased

import io.prediction.commons.modeldata.Config
import io.prediction.commons.settings.{Algo, App}

object ItemRecKNNItemBasedAlgoBatchOutput {
  val config = new Config()
  val itemRecScores = config.getItemRecScores()

  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo) = {
    itemRecScores.getTopN(uid, n, itypes) map { _.iid } toList
  }
}
