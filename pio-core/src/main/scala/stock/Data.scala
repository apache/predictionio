package io.prediction.stock

import io.prediction.{ BaseEvaluationParams, BaseTrainingDataParams,
  BaseEvaluationDataParams, BaseTrainingData, BaseFeature,
  BaseTarget, BaseModel }

import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._
import breeze.linalg.{DenseMatrix, DenseVector}

  

// Use data after baseData.
// Afterwards, slicing uses idx
// Evaluate fromIdx until untilIdx
// Use until fromIdx to construct training data
/*
case class EvaluationParams (
  baseDate: DateTime,
  fromIdx: Int,
  untilIdx: Int,
  trainingWindowSize: Int,
  evaluationInterval: Int,
  marketTicker: String,
  tickerList: Seq[String]
)
*/
class EvaluationParams(
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val trainingWindowSize: Int,
  val evaluationInterval: Int,
  val marketTicker: String,
  val tickerList: Seq[String]
) extends BaseEvaluationParams {}

class TrainingDataParams(
  val baseDate: DateTime,
  val untilIdx: Int,
  val windowSize: Int,
  val marketTicker: String,
  val tickerList: Seq[String]
) extends BaseTrainingDataParams {}

// Evaluate with data generate up to idx (exclusive). The target data is also
// restricted by idx. For example, if idx == 10, the data-preparator use data to
// at most time (idx - 1).
// EvluationDataParams specifies idx where idx in [fromIdx, untilIdx).
class EvaluationDataParams(
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val marketTicker: String,
  val tickerList: Seq[String]
) extends BaseEvaluationDataParams {}

class TrainingData (
  val price: Frame[DateTime, String, Double]
) extends BaseTrainingData {}

class Model (
  val data: Map[String, DenseVector[Double]]
) extends BaseModel {}

class Feature (
  // This is different from TrainingData. This serves as input for algorithm.
  // Hence, the time series should be shorter than that of TrainingData.
  val data: Frame[DateTime, String, Double]
) extends BaseFeature {}

class Target (
  val data: Map[String, Double]
) extends BaseTarget {}

