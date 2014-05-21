package io.prediction.engines.itemrank

import io.prediction.{ Server, BaseServerParams }

class ItemRankServer extends Server[Feature, Target, BaseServerParams] {

  override def combine(feature: Feature, target: Seq[Target]): Target = {
    target.head
  }
}
