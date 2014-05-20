package io.prediction.stock

import io.prediction.storage.Config
import io.prediction.storage.{ ItemTrend, ItemTrends }
import io.prediction.DataPreparator
import io.prediction.EvaluationPreparator
import io.prediction.PIOSettings

import scala.math
import org.saddle._
import org.saddle.index.IndexTime

import scala.collection.mutable.{ Map => MMap }
import com.github.nscala_time.time.Imports._

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.stats.{ mean, meanAndVariance }
import nak.regress.LinearRegression
import scala.collection.mutable.ArrayBuffer


class StockPreparator 
extends DataPreparator[TrainingDataParams, TrainingData]
with EvaluationPreparator[EvaluationDataParams, Feature, Target] {
  val config = new Config()
  val appid = PIOSettings.appid
  val itemTrendsDb = config.getAppdataItemTrends()
  val itemTrendsDbGetTicker = itemTrendsDb.get(appid, _:String).get
  
  def getTimeIndex(baseDate: DateTime, marketTicker: String) = {
    val spy = itemTrendsDbGetTicker(marketTicker)
    val timestamps = spy.daily.map(_._1).distinct.filter(_ >= baseDate)
    val index = IndexTime(timestamps:_*)
    index
  }

  def getPriceSeriesFromItemTrend(
    timeIndex: IndexTime,
    itemTrend: ItemTrend) : (
    Series[DateTime, Double], Series[DateTime, Boolean]) = {
    // The current implementation imports the whole series and reindex with the
    // input timeIndex. Of course not the most efficient one. May revisit later.
    val daily = itemTrend.daily

    val timestamps: IndexTime = IndexTime(daily.map(_._1):_*)

    val aprice = daily.map(_._7).toArray
    val active = daily.map(_._8).toArray

    val apriceSeries: Series[DateTime, Double] = 
      Series(Vec(aprice), timestamps).reindex(timeIndex)
    val activeSeries = Series(Vec(active), timestamps).reindex(timeIndex)

    (apriceSeries, activeSeries)
  }

  // getData return Option[Series]. It is None when part of the data is not
  // clean within timeIndex.
  private def getData(timeIndex: IndexTime, ticker: String) = {
    val itemTrend = itemTrendsDbGetTicker(ticker)
    val (price, active) = getPriceSeriesFromItemTrend(timeIndex, itemTrend)
    val allActive = active.values.foldLeft(true)(_ && _)
    // Return None if the data is not clean enough for training
    (if (allActive) Some(price) else None)
  }
 
  def prepareTraining(params: TrainingDataParams): TrainingData = {
    val timeIndex = getTimeIndex(params.baseDate, params.marketTicker).slice(
      params.untilIdx - params.windowSize, params.untilIdx)
    val tickerDataSeq = params.tickerList
    .map(ticker => (ticker, getData(timeIndex, ticker)))
    .filter{ case(ticker, optData) => !optData.isEmpty }
    .map{ case(ticker, optData) => (ticker, optData.get) }
    new TrainingData(price = Frame(tickerDataSeq:_*))
  }

  // Generate evaluation data set with target data up to idx (exclusive)
  // e.g. idx = 5, window_size = 3, data = [3,4,5,6,7,8,9,10,11,12]
  // The data visible to this function should be everything up to idx = 5:
  //   visible_data = [3,4,5,6,7]
  //   feature use [4,5,6]
  //   target use [6,7], (as target represents daily return)
  def prepareOneEvaluation(idx: Int,
    baseDate: DateTime,
    marketTicker: String, tickerList: Seq[String]): (Feature, Target) = {
    val featureWindowSize = 60  // engine-specific param

    val featureFromIdx = idx - 1 - featureWindowSize
    val featureUntilIdx = idx - 1

    val timeIndex = getTimeIndex(baseDate, marketTicker)
      .slice(featureFromIdx, idx)
      
    // generate (ticker, feature, target)-tuples
    val data = tickerList
    .map(ticker => (ticker, getData(timeIndex, ticker)))
    .filter{ case(ticker, optPrice) => !optPrice.isEmpty }
    .map{ case(ticker, optPrice) => {
      val price = optPrice.get
      // feature
      val featurePrice = price.slice(0, featureWindowSize)
      // target
      val todayPrice = price.at(featureWindowSize - 1)
      val tomorrowPrice = price.at(featureWindowSize)
      val target = math.log(tomorrowPrice) - math.log(todayPrice)
      (ticker, featurePrice, target)
    }}

    val featureData = Frame(data.map(e => (e._1, e._2)):_*)
    val targetData = data.map(e => (e._1, e._3)).toMap
    return (new Feature(data = featureData), new Target(data = targetData))
  }

  def prepareEvaluation(params: EvaluationDataParams): Seq[(Feature, Target)]={
    (params.fromIdx until params.untilIdx).map( idx => 
      prepareOneEvaluation(idx, params.baseDate,
        params.marketTicker, params.tickerList)
    ).toSeq
  }
}
