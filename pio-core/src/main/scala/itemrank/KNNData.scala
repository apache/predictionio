package io.prediction.itemrank

import io.prediction.{
  BaseModel,
  BaseAlgoParams }

class KNNAlgoParams (

) extends BaseAlgoParams {}

class KNNModel (
  /*val userSeen: Map[String, SparseVector[Boolean]],
  val userHistory: Map[String, SparseVector[Int]],
  val itemSim: Map[String, SparseVector[Double]]*/
  val userSeen: Map[String, Set[String]],
  val userHistory: Map[String, Set[(String, Int)]],
  val itemSim: Map[String, Seq[(String, Double)]]
) extends BaseModel {

  override def toString = s"${itemSim}"
}
