package io.prediction.engines.stock2

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._

import _root_.io.prediction.engines.stock.SaddleWrapper

class RawData(
  val tickers: Array[String],
  val mktTicker: String,
  val timeIndex: Array[DateTime],
  private[stock2] val _price: Array[(String, Array[Double])],
  private[stock2] val _active: Array[(String, Array[Boolean])])
    extends Serializable {

  lazy val _priceFrame: Frame[DateTime, String, Double] =
    SaddleWrapper.ToFrame(timeIndex, _price)

  // FIXME. Fill NA of result.
  lazy val _retFrame: Frame[DateTime, String, Double] = 
    _priceFrame.shift(1) / _priceFrame
  
  lazy val _activeFrame: Frame[DateTime, String, Boolean] =
    SaddleWrapper.ToFrame(timeIndex, _active)

  def view(idx: Int, maxWindowSize: Int): DataView = 
    DataView(this, idx, maxWindowSize)

  override def toString(): String = {
    val timeHead = timeIndex.head
    val timeLast = timeIndex.last
    s"RawData[$timeHead, $timeLast, $mktTicker, size=${tickers.size}]"
  }
}

// A data view of RawData from [idx - maxWindowSize + 1 : idx]
// Notice that the last day is *inclusive*.
// This clas takes the whole RawData reference, hence should *not* be serialized
case class DataView(val rawData: RawData, val idx: Int, val maxWindowSize: Int) {
  def today(): DateTime = rawData.timeIndex(idx)

  val tickers = rawData.tickers
  val mktTicker = rawData.mktTicker
  
  def priceFrame(windowSize: Int = 1)
  : Frame[DateTime, String, Double] = {
    // Check windowSize <= maxWindowSize
    rawData._priceFrame.rowSlice(idx - windowSize + 1, idx + 1)
  }
  
  def retFrame(windowSize: Int = 1)
  : Frame[DateTime, String, Double] = {
    // Check windowSize <= maxWindowSize
    rawData._retFrame.rowSlice(idx - windowSize + 1, idx + 1)
  }
  
  def activeFrame(windowSize: Int = 1)
  : Frame[DateTime, String, Boolean] = {
    // Check windowSize <= maxWindowSize
    rawData._activeFrame.rowSlice(idx - windowSize + 1, idx + 1)
  }

  override def toString(): String = {
    priceFrame().toString
  }
}


// Training data visible to the user is [untilIdx - windowSize, untilIdx).
case class TrainingData(
  val untilIdx: Int,
  val maxWindowSize: Int,
  val rawDataB: Broadcast[RawData])
  extends Serializable {
 
  def view(): DataView = DataView(rawDataB.value, untilIdx - 1, maxWindowSize)
}

case class DataParams(val rawDataB: Broadcast[RawData]) extends Serializable

// Date
case class QueryDate(val idx: Int) extends Serializable {}

case class Query(
  val idx: Int,
  val dataView: DataView,
  val tickers: Array[String],
  val mktTicker: String)


// Prediction
case class Prediction(val data: Map[String, Double])
  extends Serializable {}

/*
case class PredictionActual(
  val idx: Int,
  val prediction: Prediction,
  val actual: Map[String, Double])
*/
