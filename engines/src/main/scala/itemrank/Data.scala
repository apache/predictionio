package io.prediction.engines.itemrank

import com.github.nscala_time.time.Imports._

import io.prediction.BaseParams

// param to evaluator, it applies to both DataPrep and Validator
class EvalParams(
    val appid: Int,
    // default None to include all itypes
    val itypes: Option[Set[String]] = None,
    // action for training
    val actions: Set[String],
    val hours: Int,
    val trainStart: DateTime,
    val testStart: DateTime,
    val testUntil: DateTime,
    val goal: Set[String]
  ) extends BaseParams {

  override def toString = s"appid=${appid},itypes=${itypes}" +
    s"actions=${actions}"
}

class ValidatorParams(
  val verbose: Boolean // print report
) extends BaseParams {}

// param for preparing training
class TrainDataPrepParams(
    val appid: Int,
    val itypes: Option[Set[String]],
    // action for training
    val actions: Set[String],
    // actions within this startUntil time will be included in training
    // use all data if None
    val startUntil: Option[Tuple2[DateTime, DateTime]]
  ) extends BaseParams {
    override def toString = s"${appid} ${itypes} ${actions} ${startUntil}"
  }

// param for preparing evaluation data
class ValidationDataPrepParams(
    val appid: Int,
    val itypes: Option[Set[String]],
    // actions within this startUntil time will be included in validation
    val startUntil: Tuple2[DateTime, DateTime],
    val goal: Set[String] // action name
  ) extends BaseParams {
    override def toString = s"${appid} ${itypes}" +
      s" ${actions} ${startUntil} ${goal}"
  }

class CleanserParams (
  // how to map selected actions into rating value
  // use None if use u2iActions.v field
  val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
  val conflict: String // conflict resolution, "latest" "highest" "lowest"
) extends BaseParams {
  override def toString = s"${actions} ${conflict}"
}

class ItemTD(
  val iid: String,
  val itypes: Seq[String],
  val starttime: Option[Long],
  val endtime: Option[Long],
  val inactive: Boolean) extends Serializable {
    override def toString = s"${iid}"
  }

class UserTD(
  val uid: String
) extends Serializable {
  override def toString = s"${uid}"
}

class U2IActionTD(
  val uindex: Int,
  val iindex: Int,
  val action: String, // action name
  val v: Option[Int],
  val t: Long // action time
) extends Serializable {
  override def toString = s"${uindex} ${iindex} ${action}"
}

class TrainingData(
    val users: Map[Int, UserTD], // uindex->uid
    val items: Map[Int, ItemTD], // iindex->itemTD
    val u2iActions: Seq[U2IActionTD]
  ) extends Serializable {
    override def toString = s"TrainingData: ${u2iActions} ${users}"
  }

class RatingTD(
  val uindex: Int,
  val iindex: Int,
  val rating: Int,
  val t: Long) extends Serializable {
    override def toString = s"RatingTD: ${uindex} ${iindex} ${rating}"
  }

class CleansedData(
  val users: Map[Int, UserTD],
  val items: Map[Int, ItemTD],
  val rating: Seq[RatingTD]
) extends Serializable {
  override def toString = s"CleansedData: ${rating}"
}

// "Feature" here means prediction Input
class Feature(
    val uid: String,
    val items: Seq[String] // items to be ranked
    ) extends Serializable {
  override def toString = s"[${uid}, ${items}]"
}

// prediction output
class Prediction(
  // the ranked items and score
    val items: Seq[(String, Double)]) extends Serializable {
  override def toString = s"${items}"
}

class Actual(
    // actual items the user has performed actions on
    val items: Seq[String]) extends Serializable {
  override def toString = s"${items}"
}

class ValidationUnit(
  val f: Feature,
  val p: Prediction,
  val a: Actual,
  val score: Double,
  val baseline: Double
) extends Serializable

class ValidationResult(
  val testStartUntil: Tuple2[DateTime, DateTime],
  val baselineMean: Double,
  val baselineStdev: Double,
  val algoMean: Double,
  val algoStdev: Double
) extends Serializable

class CrossValidationResult(
  val baselineMean: Double,
  val baselineStdev: Double,
  val algoMean: Double,
  val algoStdev: Double
) extends Serializable
