package io.prediction.engines.stock

import io.prediction.{
  BaseTrainingDataParams,
  BaseEvaluationDataParams,
  BaseValidationDataParams,
  BaseTrainingData,
  BaseCleansedData,
  BaseFeature,
  BasePrediction,
  BaseActual,
  BaseModel,
  BaseValidationUnit,
  BaseValidationResults,
  BaseValidationParams,
  BaseCrossValidationResults
}

import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._
import breeze.linalg.{ DenseMatrix, DenseVector }
import com.twitter.chill.MeatLocker
//import com.github.nscala_time.time.Imports._

// Use data after baseData.
// Afterwards, slicing uses idx
// Evaluate fromIdx until untilIdx
// Use until fromIdx to construct training data
class EvaluationDataParams(
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val trainingWindowSize: Int,
  val evaluationInterval: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends BaseEvaluationDataParams {}

class TrainingDataParams(
  val baseDate: DateTime,
  val untilIdx: Int,
  val windowSize: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends BaseTrainingDataParams {}

// Evaluate with data generate up to idx (exclusive). The target data is also
// restricted by idx. For example, if idx == 10, the data-preparator use data to
// at most time (idx - 1).
// EvluationDataParams specifies idx where idx in [fromIdx, untilIdx).
class ValidationDataParams(
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends BaseValidationDataParams {}

class TrainingData(
  val tickers: Seq[String],
  val mktTicker: String,
  val data: (Array[DateTime], Array[(String, Array[Double])]))
  extends BaseTrainingData {
  val timeIndex: Array[DateTime] = data._1
  val tickerPriceSeq: Array[(String, Array[Double])] = data._2
 
  @transient lazy val price = SaddleWrapper.ToFrame(timeIndex, tickerPriceSeq)

  override def toString(): String = {
    val firstDate = timeIndex.head
    val lastDate = timeIndex.last
    s"TrainingData $firstDate $lastDate"
  }
}

object TrainingData {
  def apply(
    tickers: Seq[String],
    mktTicker: String,
    price: Frame[DateTime, String, Double]): TrainingData = {
    return new TrainingData(
      tickers,
      mktTicker,
      SaddleWrapper.FromFrame(price))
  }
}

object SaddleWrapper {
  def ToFrame(
    timeIndex: Array[DateTime],
    tickerPriceSeq: Array[(String, Array[Double])]
    ): Frame[DateTime, String, Double] = {
    val index = IndexTime(timeIndex:_ *)
    val seriesList = tickerPriceSeq.map{ case(ticker, price) => {
      val series = Series(Vec(price), index)
      (ticker, series)
    }}
    Frame(seriesList:_*)
  }

  def FromFrame(data: Frame[DateTime, String, Double]
    ): (Array[DateTime], Array[(String, Array[Double])]) = {
    val timeIndex = data.rowIx.toVec.contents
    val tickers = data.colIx.toVec.contents

    val tickerDataSeq = tickers.map{ ticker => {
      (ticker, data.firstCol(ticker).toVec.contents)
    }}

    (timeIndex, tickerDataSeq)
  }
}



class Model(
  val data: Map[String, DenseVector[Double]]) extends BaseModel {}

// This is different from TrainingData. This serves as input for algorithm.
// Hence, the time series should be shorter than that of TrainingData.
class Feature(
  val input: (Array[DateTime], Array[(String, Array[Double])])
  ) extends BaseFeature {
  
  val timeIndex: Array[DateTime] = input._1
  val tickerPriceSeq: Array[(String, Array[Double])] = input._2
  @transient lazy val data = SaddleWrapper.ToFrame(timeIndex, tickerPriceSeq)
  //val data = SaddleWrapper.ToFrame(timeIndex, tickerPriceSeq)

  def today: DateTime = data.rowIx.last.get

  override def toString(): String = {
    val firstDate = data.rowIx.first.get
    val lastDate = data.rowIx.last.get
    //val firstDate = data.get.rowIx.first.get
    //val lastDate = data.get.rowIx.last.get
    s"Feature $firstDate $lastDate"
  }
}

object Feature {
  def apply(
    data: Frame[DateTime, String, Double]): Feature = {
    return new Feature(SaddleWrapper.FromFrame(data))
  }
}

class Target(
  val data: Map[String, Double]) extends BasePrediction with BaseActual {}

class ValidationUnit(
  val data: Seq[(Double, Double)]) extends BaseValidationUnit {}

class ValidationResults(
  val vuSeq: Seq[ValidationUnit]) extends BaseValidationResults {}

class CrossValidationResults(
  val s: String) extends BaseCrossValidationResults {
  override def toString() = s
}




