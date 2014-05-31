package io.prediction.engines.itemrank

import com.github.nscala_time.time.Imports._

import io.prediction.{
  //BaseEvaluationParams,
  BaseValidationParams,
  BaseEvaluationDataParams,
  BaseTrainingDataParams,
  BaseValidationDataParams,
  BaseTrainingData,
  BaseFeature,
  BasePrediction,
  BaseActual,
  BaseModel,
  BaseAlgoParams,
  BaseValidationUnit,
  BaseValidationResults,
  BaseCrossValidationResults
}

// param to evaluator
class EvalParams(
    // TODO
    //val iterations: Int
    val appid: Int,
    val itypes: Option[Set[String]],
    // action for training
    val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
    val conflict: String, // conflict resolution, "latest" "highest" "lowest"
    //val recommendationTime: Long,
    val seenActions: Option[Set[String]], // (view, rate)
    //val ignoreInactive: Boolean,
    val hours: Int,
    val trainStart: DateTime,
    val testStart: DateTime,
    val testUntil: DateTime,
    val goal: Set[String]
  //) extends BaseEvaluationParams {
  ) extends BaseEvaluationDataParams with BaseValidationParams {

  override def toString = s"appid=${appid},itypes=${itypes}" +
    s"actions=${actions}, conflict=${conflict}"
}


// param for preparing training
class TrainDataPrepParams(
    val appid: Int,
    val itypes: Option[Set[String]],
    val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
    val conflict: String, // conflict resolution, "latest" "highest" "lowest"
    //val recommendationTime: Long,
    val seenActions: Option[Set[String]], // (view, rate)
    //val ignoreInactive: Boolean
    // use all data if None
    val startUntil: Option[Tuple2[DateTime, DateTime]]
  ) extends BaseTrainingDataParams {
    override def toString = {
      startUntil.map( x => s"start=${x._1} until=${x._2}").getOrElse("All")
    }
  }

// param for preparing evaluation data
class EvalDataPrepParams(
    val appid: Int,
    val itypes: Option[Set[String]],
    val startUntil: Tuple2[DateTime, DateTime],
    val goal: Set[String] // action name
  ) extends BaseValidationDataParams {
    override def toString = s"start=${startUntil._1} until=${startUntil._2}"
  }

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
    val items: Seq[String] // items to be ranked
    ) extends BaseFeature {
  override def toString = s"[${uid}, ${items}]"
}

// prediction output
class Prediction(
  // the ranked items and score
    val items: Seq[(String, Double)]) extends BasePrediction {
  override def toString = s"${items}"
}

class Actual(
    // actual items the user has performed actions on
    val items: Seq[String]) extends BaseActual {
  override def toString = s"${items}"
}

class EvalUnit(
  val f: Feature,
  val p: Prediction,
  val a: Actual,
  val score: Double,
  val baseline: Double
) extends BaseValidationUnit {}

class EvalResults() extends BaseValidationResults {}

class CrossEvalResults() extends BaseCrossValidationResults{} 

