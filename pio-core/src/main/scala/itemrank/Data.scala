package io.prediction.itemrank

import io.prediction.{
  BaseEvaluationParams,
  BaseTrainingDataParams,
  BaseEvaluationDataParams,
  BaseTrainingData,
  BaseFeature,
  BaseTarget,
  BaseModel,
  BaseAlgoParams }

class EvalParams (
  // TODO
  //val iterations: Int
  val appid: Int,
  val itypes: Option[Set[String]],
  val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
  val conflict: String, // conflict resolution, "latest" "highest" "lowest"
  val recommendationTime: Long,
  val seenActions: Option[Set[String]], // (view, rate)
  //val ignoreInactive: Boolean,
  val testUsers: Set[String],
  val testItems: Set[String],
  val goal: Set[String]
) extends BaseEvaluationParams {

  override def toString = s"appid=${appid},itypes=${itypes}" +
    s"actions=${actions}, conflict=${conflict}"
}

class TrainDataParams (
  val appid: Int,
  val itypes: Option[Set[String]],
  val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
  val conflict: String, // conflict resolution, "latest" "highest" "lowest"
  val recommendationTime: Long,
  val seenActions: Option[Set[String]] // (view, rate)
  //val ignoreInactive: Boolean
) extends BaseTrainingDataParams {}

class EvalDataParams (
  val testUsers: Set[String],
  val testItems: Set[String],
  val goal: Set[String] // action name
) extends BaseEvaluationDataParams {}

class ItemTD (
  val iid: String,
  val itypes: Seq[String],
  val starttime: Option[Long],
  val endtime: Option[Long],
  val inactive: Boolean
)

class RatingTD (
  val uindex: Int,
  val iindex: Int,
  val rating: Int,
  val t: Long
)

class TrainigData (
  val users: Map[Int, String], // uindex->uid
  val items: Map[Int, ItemTD], // iindex->itemTD
  //val possibleItems: Set[Int], // iindex
  val rating: Seq[RatingTD],
  val seen: Set[(Int, Int)] // uindex->iindex
) extends BaseTrainingData {}

class AlgoParams (

) extends BaseAlgoParams {}

class Model (
  /*val userSeen: Map[String, SparseVector[Boolean]],
  val userHistory: Map[String, SparseVector[Int]],
  val itemSim: Map[String, SparseVector[Double]]*/
  val userSeen: Map[String, Set[String]],
  val userHistory: Map[String, Set[(String, Int)]],
  val itemSim: Map[String, Seq[(String, Double)]]
) extends BaseModel {

  override def toString = s"${itemSim}"
}

// "Feature" here means prediction Input
class Feature (
  val uid: String,
  val items: Set[String] // items to be ranked
) extends BaseFeature {
  override def toString = s"[${uid}, ${items}]"
}

// target means prediction output
class Target (
  val items: Seq[(String, Double)]
) extends BaseTarget {
  override def toString = s"${items}"
}
