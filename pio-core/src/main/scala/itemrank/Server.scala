package io.prediction.itemrank

import io.prediction.{ BaseServer }

class Server extends BaseServer[Feature, Target] {

  override def combine(feature: Feature, target: Seq[Target]): Target = {
    target.head
  }
}
