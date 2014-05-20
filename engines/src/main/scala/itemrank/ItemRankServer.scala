package io.prediction.engines.itemrank

import io.prediction.{ Server }

class ItemRankServer extends Server[Feature, Target] {

  override def combine(feature: Feature, target: Seq[Target]): Target = {
    target.head
  }
}
