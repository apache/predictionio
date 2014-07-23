package io.prediction.engines.stock

import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._
import breeze.linalg.{ DenseMatrix, DenseVector }

class TrainingData(
  val tickers: Seq[String],
  val mktTicker: String,
  val timeIndex: Array[DateTime],
  val price: Array[(String, Array[Double])])
  extends Serializable {
 
  @transient lazy val priceFrame = SaddleWrapper.ToFrame(timeIndex, price)

  override def toString(): String = {
    val firstDate = timeIndex.head
    val lastDate = timeIndex.last
    s"TrainingData [$firstDate, $lastDate]"
  }
}

object TrainingData {
  def apply(
    tickers: Seq[String],
    mktTicker: String,
    priceFrame: Frame[DateTime, String, Double]): TrainingData = {
  
    val data: (Array[DateTime], Array[(String, Array[Double])]) =
      SaddleWrapper.FromFrame(priceFrame)
    return new TrainingData(tickers, mktTicker, data._1, data._2)
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

// This is different from TrainingData. This serves as input for algorithm.
// Hence, the time series should be shorter than that of TrainingData.
class Feature(
  val mktTicker: String,
  val tickerList: Seq[String],
  val input: (Array[DateTime], Array[(String, Array[Double])]),
  val tomorrow: DateTime
  ) extends Serializable {
  
  val timeIndex: Array[DateTime] = input._1
  val tickerPriceSeq: Array[(String, Array[Double])] = input._2
  @transient lazy val data = SaddleWrapper.ToFrame(timeIndex, tickerPriceSeq)

  def today: DateTime = data.rowIx.last.get

  override def toString(): String = {
    val firstDate = data.rowIx.first.get
    val lastDate = data.rowIx.last.get
    s"Feature [$firstDate, $lastDate]"
  }
}

object Feature {
  def apply(
    mktTicker: String,
    tickerList: Seq[String],
    data: Frame[DateTime, String, Double],
    tomorrow: DateTime): Feature = {
    return new Feature(
      mktTicker = mktTicker,
      tickerList = tickerList,
      input = SaddleWrapper.FromFrame(data),
      tomorrow = tomorrow
      )
  }
}
