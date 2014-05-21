package myengine

import io.prediction.BaseServerParams
import io.prediction.Server

class MyServerParams() extends BaseServerParams {}

class MyServer extends Server[Feature, Target, MyServerParams] {

  def combine(feature: Feature, targets: Seq[Target]): Target = new Target()

}
