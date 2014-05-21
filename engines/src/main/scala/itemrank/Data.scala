package io.prediction.engines.itemrank

import io.prediction.{
  BaseEvaluationParams,
  BaseTrainingDataParams,
  BaseEvaluationDataParams,
  BaseTrainingData,
  BaseFeature,
  BaseTarget,
  BaseModel,
  BaseAlgoParams,
  BaseEvaluationUnit
}

// param to evaluator
class EvalParams(
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
    val goal: Set[String]) extends BaseEvaluationParams {

  override def toString = s"appid=${appid},itypes=${itypes}" +
    s"actions=${actions}, conflict=${conflict}"
}

// param for preparing training
class TrainDataParams(
  val appid: Int,
  val itypes: Option[Set[String]],
  //val start: Option[DateTime],
  //val until: Option[DataTime],// use all data if both start and until is None
  val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
  val conflict: String, // conflict resolution, "latest" "highest" "lowest"
  val recommendationTime: Long,
  val seenActions: Option[Set[String]] // (view, rate)
  //val ignoreInactive: Boolean
  ) extends BaseTrainingDataParams {}

// param for preparing evaluation data
class EvalDataParams(
  val testUsers: Set[String],
  val testItems: Set[String],
  val goal: Set[String] // action name
  ) extends BaseEvaluationDataParams {}

class ItemTD(
  val iid: String,
  val itypes: Seq[String],
  val starttime: Option[Long],
  val endtime: Option[Long],
  val inactive: Boolean)

class RatingTD(
  val uindex: Int,
  val iindex: Int,
  val rating: Int,
  val t: Long)

class TrainigData(
  val users: Map[Int, String], // uindex->uid
  val items: Map[Int, ItemTD], // iindex->itemTD
  //val possibleItems: Set[Int], // iindex
  val rating: Seq[RatingTD],
  val seen: Set[(Int, Int)] // uindex->iindex
  ) extends BaseTrainingData {}

// "Feature" here means prediction Input
class Feature(
    val uid: String,
    val items: Set[String] // items to be ranked
    ) extends BaseFeature {
  override def toString = s"[${uid}, ${items}]"
}

// target means prediction output
class Target(
    val items: Seq[(String, Double)]) extends BaseTarget {
  override def toString = s"${items}"
}

class EvalUnit(val f: Feature, val p: Target) extends BaseEvaluationUnit {}
