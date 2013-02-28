package io.prediction.output

import io.prediction.commons.settings.Algo

trait ItemSimAlgoOutput {
  def output(iid: String, itypes: Option[List[String]])(implicit algo: Algo): Seq[String]
}

object ItemSimAlgoOutput {
  def output(iid: String, itypes: Option[List[String]])(implicit algo: Algo): Seq[String] = {
    algo.infoid match {
      case "io.prediction.algorithms.scalding.itemsim.itemsimcf" => ItemSimCFAlgoOutput.output(iid, itypes)
      case _ => throw new RuntimeException("Unsupported itemsim algorithm package: %s" format algo.infoid)
    }
  }
}
