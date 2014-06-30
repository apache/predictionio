package io.prediction.engines.itemrank

import io.prediction.BaseParams

class KNNAlgoParams(
  val similarity: String,
  val k: Int
) extends BaseParams {
  override def toString = s"${similarity} ${k}"
}

class KNNModel(
    //val userSeen: Map[String, Set[String]],
    val userHistory: Map[String, Set[(String, Int)]],
    val itemSim: Map[String, Seq[(String, Double)]]) extends Serializable {

  override def toString = s"${itemSim}"
}
