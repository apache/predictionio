package io.prediction.engines.stock

import io.prediction.controller.Params
import io.prediction.controller.LSlicedDataSource
import com.github.nscala_time.time.Imports._
import io.prediction.storage.Storage
import io.prediction.storage.{ ItemTrend, ItemTrends }

import scala.math
// FIXME(yipjustin). Remove ._ as it is polluting the namespace.
import org.saddle._
import org.saddle.index.IndexTime

import scala.collection.mutable.{ Map => MMap }
import com.github.nscala_time.time.Imports._

import breeze.linalg.{ DenseMatrix, DenseVector }
import breeze.stats.{ mean, meanAndVariance }
import nak.regress.LinearRegression
import scala.collection.mutable.ArrayBuffer
import com.twitter.chill.MeatLocker

/** Primary parameter for [[[StockDataSource]]].
  *
  * @param baseDate identify the beginning of our global time window, and
  * the rest are use index. 
  * @param fromIdx the first date for testing
  * @param untilIdx the last date (exclusive) for testing
  * @param trainingWindowSize number of days used for training
  * @param evaluationInterval number of days for each testing data
  *
  * [[[StockDataSource]]] chops data into (overlapping) multiple
  * pieces. Within each piece, it is further splitted into training and testing
  * set. The testing sets is from <code>fromIdx</code> until
  * <code>untilIdx</code> with a step size of <code>evaluationInterval</code>.
  * A concrete example: (from, until, windowSize) = (100, 150, 20), it generates
  * three testing sets corresponding to time range: [100, 120), [120, 140), [140, 150).
  * For each testing sets, it also generates the training data set using
  * <code>trainingWindowSize</code>. Suppose windowSize = 50 and testing set =
  * [100, 120), the training set draws data in time range [50, 100).
  */
case class DataSourceParams(
  val appid: Int = 42,
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val trainingWindowSize: Int,
  val evaluationInterval: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends Params {}

case class TrainingDataParams2(
  val baseDate: DateTime,
  val untilIdx: Int,
  val windowSize: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends Serializable {}

// Testing data generate up to idx (exclusive). The target data is also
// restricted by idx. For example, if idx == 10, the data-preparator use data to
// at most time (idx - 1).
// TestingDataParams specifies idx where idx in [fromIdx, untilIdx).
case class TestingDataParams(
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends Serializable {}

class Target(
  val date: DateTime,
  val data: Map[String, Double]) extends Serializable {
  override def toString(): String = s"Target @$date"
}

/** StockDataSource generates a series of training / testing data pairs.
  *
  * Main parameter: [[[DataSourceParams]]]
  *
  * 
  */
class StockDataSource(val dsp: DataSourceParams)
  extends LSlicedDataSource[
    DataSourceParams,
    (TrainingDataParams2, TestingDataParams),
    TrainingData,
    Feature,
    Target] {
  @transient lazy val itemTrendsDbGetTicker = 
    Storage.getAppdataItemTrends().get(dsp.appid, _: String).get
  @transient lazy val itemTrendsDb = Storage.getAppdataItemTrends()

  def generateDataParams(): Seq[(TrainingDataParams2, TestingDataParams)] = {
    Range(dsp.fromIdx, dsp.untilIdx, dsp.evaluationInterval)
      .map(idx => {
        val trainParams = new TrainingDataParams2(
          baseDate = dsp.baseDate,
          untilIdx = idx - 1,
          windowSize = dsp.trainingWindowSize,
          marketTicker = dsp.marketTicker,
          tickerList = dsp.tickerList)
        val testingParams = new TestingDataParams(
          baseDate = dsp.baseDate,
          fromIdx = idx,
          untilIdx = math.min(
            idx + dsp.evaluationInterval,
            dsp.untilIdx),
          marketTicker = dsp.marketTicker,
          tickerList = dsp.tickerList)
        (trainParams, testingParams)
      })
  }

  def read(p: (TrainingDataParams2, TestingDataParams))
  : (TrainingData, Seq[(Feature, Target)]) = {
    return (prepareTraining(p._1), prepareValidation(p._2))
  }
  
  def getTimeIndex(baseDate: DateTime, marketTicker: String) = {
    val spy = itemTrendsDbGetTicker(marketTicker)
    val timestamps = spy.daily.map(_._1).distinct.filter(_ >= baseDate)
    val index = IndexTime(timestamps: _*)
    index
  }

  def getPriceSeriesFromItemTrend(
    timeIndex: IndexTime,
    itemTrend: ItemTrend)
  : (Series[DateTime, Double], Series[DateTime, Boolean]) = {
    // The current implementation imports the whole series and reindex with the
    // input timeIndex. Of course not the most efficient one. May revisit later.
    val daily = itemTrend.daily

    val timestamps: IndexTime = IndexTime(daily.map(_._1): _*)

    val aprice = daily.map(_._7).toArray
    val active = daily.map(_._8).toArray

    val apriceSeries: Series[DateTime, Double] =
      Series(Vec(aprice), timestamps).reindex(timeIndex)
    val activeSeries = Series(Vec(active), timestamps).reindex(timeIndex)

    (apriceSeries, activeSeries)
  }

  // getData return Option[Series]. It is None when part of the data is not
  // clean within timeIndex.
  private def getData(itemTrend: ItemTrend, timeIndex: IndexTime,
    ticker: String): Option[Series[DateTime, Double]] = {
    val (price, active) = getPriceSeriesFromItemTrend(timeIndex, itemTrend)
    val allActive = active.values.foldLeft(true)(_ && _)
    // Return None if the data is not clean enough for training
    (if (allActive) Some(price) else None)
  }

  private def getBatchItemTrend(tickers: Seq[String])
    : Map[String, ItemTrend] = {
    itemTrendsDb.getByIds(dsp.appid, tickers).map{ e => {
      (e.id, e)
    }}.toMap
  }


  def prepareTraining(params: TrainingDataParams2): TrainingData = {
    val timeIndex = getTimeIndex(params.baseDate, params.marketTicker).slice(
      params.untilIdx - params.windowSize, params.untilIdx)

    val allTickers = params.tickerList :+ params.marketTicker

    val tickerTrendMap = getBatchItemTrend(allTickers)

    val tickerDataSeq = allTickers
      .map(t => (t, getData(tickerTrendMap(t), timeIndex, t)))
      .filter { case (ticker, optData) => !optData.isEmpty }
      .map { case (ticker, optData) => (ticker, optData.get) }
    TrainingData(
      tickers = params.tickerList,
      mktTicker = params.marketTicker,
      price = Frame(tickerDataSeq: _*))

  }

  // Generate evaluation data set with target data up to idx (exclusive)
  // e.g. idx = 5, window_size = 3, data = [3,4,5,6,7,8,9,10,11,12]
  // The data visible to this function should be everything up to idx = 5:
  //   visible_data = [3,4,5,6,7]
  //   feature use [4,5,6]
  //   target use [6,7], (as target represents daily return)
  def prepareOneValidation(
    tickerTrendMap: Map[String, ItemTrend],
    idx: Int,
    baseDate: DateTime,
    marketTicker: String,
    tickerList: Seq[String]): (Feature, Target) = {
    val allTickers: Seq[String] = marketTicker +: tickerList

    val featureWindowSize = 30 // engine-specific param

    val featureFromIdx = idx - 1 - featureWindowSize
    val featureUntilIdx = idx - 1

    val timeIndex = getTimeIndex(baseDate, marketTicker)
      .slice(featureFromIdx, idx)

    // generate (ticker, feature, target)-tuples
    val data = allTickers
      .map(t => (t, getData(tickerTrendMap(t), timeIndex, t)))
      .filter { case (ticker, optPrice) => !optPrice.isEmpty }
      .map {
        case (ticker, optPrice) => {
          val price = optPrice.get
          // feature
          val featurePrice = price.slice(0, featureWindowSize)
          // target
          val todayPrice = price.at(featureWindowSize - 1)
          val tomorrowPrice = price.at(featureWindowSize)
          val target = math.log(tomorrowPrice) - math.log(todayPrice)
          (ticker, featurePrice, target)
        }
      }

    val featureData = Frame(data.map(e => (e._1, e._2)): _*)
    val targetData = data.map(e => (e._1, e._3)).toMap

    // Only validate activeTickers
    val activeTickerList = data.map(_._1).filter(_ != marketTicker)

    return (
      Feature(
        mktTicker = marketTicker,
        tickerList = activeTickerList,
        data = featureData,
        tomorrow = timeIndex.last.get
      ),
      new Target(date = timeIndex.last.get, data = targetData))
  }

  def prepareValidation(params: TestingDataParams)
  : Seq[(Feature, Target)] = {
    val tickerList: Seq[String] = params.marketTicker +: params.tickerList
    val tickerTrendMap = getBatchItemTrend(tickerList)

    (params.fromIdx until params.untilIdx).map(idx =>
      prepareOneValidation(
        tickerTrendMap,
        idx,
        params.baseDate,
        params.marketTicker,
        params.tickerList)
    ).toSeq
  }

}
        
