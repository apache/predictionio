package io.prediction.output

import io.prediction.commons.settings.{Algo, App}

trait ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo): Seq[String]
}

object ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo): Seq[String] = {
    algo.pkgname match {
      case "io.prediction.algorithms.scalding.itemrec.knnitembased" => ItemRecKNNItemBasedAlgoOutput.output(uid, n, itypes)
      case _ => throw new RuntimeException("Unsupported itemrec algorithm package: %s" format algo.pkgname)
    }
  }
}
