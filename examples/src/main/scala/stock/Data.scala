package io.prediction.examples.stock_old

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
  def ToFrame[A](
    timeIndex: Array[DateTime],
    tickerPriceSeq: Array[(String, Array[A])]
    )(implicit st: ST[A])
  : Frame[DateTime, String, A] = {
    val index = IndexTime(timeIndex:_ *)
    val seriesList = tickerPriceSeq.map{ case(ticker, price) => {
      val series = Series(Vec(price), index)
      (ticker, series)
    }}
    Frame(seriesList:_*)
  }

  def FromFrame[A](data: Frame[DateTime, String, A]
    ): (Array[DateTime], Array[(String, Array[A])]) = {
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
class Query(
  val mktTicker: String,
  val tickerList: Seq[String],
  val timeIndex: Array[DateTime],
  val price: Array[(String, Array[Double])],
  val tomorrow: DateTime
  ) extends Serializable {
  
  @transient lazy val priceFrame = SaddleWrapper.ToFrame(timeIndex, price)

  val today: DateTime = priceFrame.rowIx.last.get

  override def toString(): String = {
    val firstDate = timeIndex.head
    val lastDate = timeIndex.last
    s"Feature [$firstDate, $lastDate]"
  }
}

object Query {
  def apply(
    mktTicker: String,
    tickerList: Seq[String],
    priceFrame: Frame[DateTime, String, Double],
    tomorrow: DateTime): Query = {
    
    val data: (Array[DateTime], Array[(String, Array[Double])]) =
      SaddleWrapper.FromFrame(priceFrame)

    return new Query(
      mktTicker = mktTicker,
      tickerList = tickerList,
      timeIndex = data._1,
      price = data._2,
      tomorrow = tomorrow
      )
  }
}

class Target(
  val date: DateTime,
  val data: Map[String, Double]) extends Serializable {
  override def toString(): String = s"Target @$date"
}
