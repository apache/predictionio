package io.prediction.output

import io.prediction.commons.settings._

object ItemSimCFAlgoBatchOutput {
  def output(iid: String, itypes: Option[List[String]])(implicit algo: Algo) = {
    Seq("itemsim", iid, algo.id.toString, "foo", "bar")
  }
}
