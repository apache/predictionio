package io.prediction.engines.itemrank

import com.github.nscala_time.time.Imports._

import io.prediction.controller.Params

// param to evaluator, it applies to both DataPrep and Validator
class DataSourceParams(
    val appid: Int,
    // default None to include all itypes
    val itypes: Option[Set[String]] = None,
    // action for training
    val actions: Set[String],
    val hours: Int,
    val trainStart: DateTime,
    val testStart: DateTime,
    val testUntil: DateTime,
    val goal: Set[String],
    val verbose: Boolean // for debug purpose
  ) extends Params {
  override def toString = s"appid=${appid},itypes=${itypes}" +
    s"actions=${actions}"
}

class MetricsParams(
  val verbose: Boolean // print report
) extends Params {}

// param for preparing training
class TrainingDataParams(
    val appid: Int,
    val itypes: Option[Set[String]],
    // action for training
    val actions: Set[String],
    // actions within this startUntil time will be included in training
    // use all data if None
    val startUntil: Option[Tuple2[DateTime, DateTime]],
    val verbose: Boolean // for debug purpose
  ) extends Serializable {
    override def toString = s"${appid} ${itypes} ${actions} ${startUntil}"
  }

// param for preparing evaluation data
class ValidationDataParams(
    val appid: Int,
    val itypes: Option[Set[String]],
    // actions within this startUntil time will be included in validation
    val startUntil: Tuple2[DateTime, DateTime],
    val goal: Set[String] // action name
  ) extends Serializable {
    override def toString = s"${appid} ${itypes} ${startUntil} ${goal}"
  }

class DataParams(
  val tdp: TrainingDataParams,
  val vdp: ValidationDataParams
) extends Params {
  override def toString = s"TDP: ${tdp} VDP: ${vdp}"
}

class PreparatorParams (
  // how to map selected actions into rating value
  // use None if use u2iActions.v field
  val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
  val conflict: String // conflict resolution, "latest" "highest" "lowest"
) extends Params {
  override def toString = s"${actions} ${conflict}"
}
