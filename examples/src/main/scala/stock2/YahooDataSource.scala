//package io.prediction.data.examples
package io.prediction.examples.stock2

// YahooDataSource reads PredictionIO event store directly.

import io.prediction.data.storage.Event
import io.prediction.data.storage.Events
import io.prediction.data.storage.Storage
import io.prediction.data.view.LBatchView
import io.prediction.data.storage.DataMap

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import com.github.nscala_time.time.Imports._

import scala.collection.mutable.{ Map => MMap }
import scala.collection.GenMap

import io.prediction.controller._
import io.prediction.controller.{ Params => BaseParams }


import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast

import org.json4s._

case class HistoricalData(
  val ticker: String,
  val timeIndex: Array[DateTime],
  val close: Array[Double],
  val adjClose: Array[Double],
  val adjReturn: Array[Double],
  val volume: Array[Double],
  val active: Array[Boolean]) extends Serializable {

  override def toString(): String = {
    s"HistoricalData($ticker, ${timeIndex.head}, ${timeIndex.last})"
  }
}



class YahooDataSource(val params: YahooDataSource.Params) 
  extends PDataSource[
      YahooDataSource.Params,
      AnyRef,
      RDD[AnyRef],
      AnyRef, 
      AnyRef] {
  @transient lazy val batchView = new LBatchView(
    params.appId, params.startTime, params.untilTime)
    
  val timezone = DateTimeZone.forID("US/Eastern")
  val marketTicker = params.windowParams.marketTicker

  val market: HistoricalData = getTimeIndex()
  //val timeIndex: Array[DateTime] = Array[DateTime](market.timeIndex:_*)
  val timeIndex: Array[DateTime] = market.timeIndex
  val timeIndexSet: Set[DateTime] = timeIndex.toSet

  def merge(intermediate: YahooDataSource.Intermediate, e: Event,
    timeIndexSetOpt: Option[Set[DateTime]]) 
  : YahooDataSource.Intermediate = {
    val dm: DataMap = e.properties     

    // TODO: Check ticker in intermediate
    
    val yahooData = dm.get[JObject]("yahoo")

    // used by json4s "extract" method.
    implicit val formats = DefaultFormats

    val closeList = (yahooData \ "close").extract[Array[Double]]
    val adjCloseList = (yahooData \ "adjclose").extract[Array[Double]]
    val volumeList = (yahooData \ "volume").extract[Array[Double]]

    val tList: Array[DateTime] = (yahooData \ "t").extract[Array[Long]]
      .map(t => new DateTime(t * 1000, timezone))

    // Add data either 
    // 1. timeIndex exists and t is in timeIndex, or 
    // 2. timeIndex is None.
    val newDailyMap: Map[DateTime, YahooDataSource.Daily] = 
      tList.zipWithIndex.drop(1)
      .filter { case (t, idx) => timeIndexSetOpt.map(_(t)).getOrElse(true) }
      .map { case (t, idx) =>
        val adjReturn = (adjCloseList(idx) / adjCloseList(idx - 1)) - 1

        val daily = YahooDataSource.Daily(
          close = closeList(idx),
          adjClose = adjCloseList(idx),
          adjReturn = adjReturn,
          volume = volumeList(idx),
          active = true,
          prevDate = tList(idx - 1))

        (t -> daily)
      }
      .toMap

    YahooDataSource.Intermediate(
      ticker = e.entityId, 
      dailyMap = intermediate.dailyMap ++ newDailyMap)
  }

  def mergeTimeIndex(intermediate: YahooDataSource.Intermediate, e: Event)
  : YahooDataSource.Intermediate = merge(intermediate, e, None)

  def mergeStock(intermediate: YahooDataSource.Intermediate, e: Event)
  : YahooDataSource.Intermediate = merge(intermediate, e, Some(timeIndexSet))

  def finalizeTimeIndex(intermediate: YahooDataSource.Intermediate)
  : HistoricalData = {
    val dailyMap = intermediate.dailyMap
    val ticker = intermediate.ticker

    // Construct the time index with windowParams
    val timeIndex: Array[DateTime] = dailyMap.keys.toArray
      .filter(_.isAfter(params.windowParams.baseDate))
      .sortBy(identity)
      .take(params.windowParams.untilIdx)

    // Check if the time is continuous
    (1 until timeIndex.size).foreach { idx => {
      require(dailyMap(timeIndex(idx)).prevDate == timeIndex(idx - 1),
        s"Time must be continuous. " +
        s"For ticker $ticker, there is a gap between " +
        s"${timeIndex(idx - 1)} and ${timeIndex(idx)}. " + 
        s"Please import data to cover the gap or use a shorter range.")
    }}

    HistoricalData(
      ticker = ticker, 
      timeIndex = timeIndex,
      close = timeIndex.map(t => dailyMap(t).close),
      adjClose = timeIndex.map(t => dailyMap(t).adjClose),
      adjReturn = timeIndex.map(t => dailyMap(t).adjReturn),
      volume = timeIndex.map(t => dailyMap(t).volume),
      active = Array.fill(timeIndex.size)(true))
  }

  // Traverse the timeIndex to construct the actual time series using dailyMap
  // and extra fillNA logic.
  //
  // The time series is constructed in the same order as the global timeIndex
  // array. For a datetime t, if dailyMap contains the data, it calls valueFunc
  // to extract the value; otherwise, it calls fillNaFunc with the optional last
  // extracted value and get the default value.
  def activeFilter[A : Manifest](
    dailyMap: GenMap[DateTime, YahooDataSource.Daily], 
    valueFunc: YahooDataSource.Daily => A, 
    fillNAFunc: Option[A] => A) : Array[A] = {

    var lastOpt: Option[A] = None
    
    timeIndex
    .map { t => 
      if (dailyMap.contains(t)) {
        val v = valueFunc(dailyMap(t)) 
        lastOpt = Some(v)
        v
      } else {
        fillNAFunc(lastOpt)
      }
    }
    .toArray
  }

  def finalizeStock(intermediate: YahooDataSource.Intermediate)
  : HistoricalData = {
    val dailyMap = intermediate.dailyMap 

    HistoricalData(
      ticker = intermediate.ticker,
      timeIndex = timeIndex,
      close = activeFilter[Double](dailyMap, _.close, _.getOrElse(0.0)),
      adjClose = activeFilter[Double](dailyMap, _.adjClose, _.getOrElse(0.0)),
      adjReturn = activeFilter[Double](dailyMap, _.adjReturn, _.getOrElse(0.0)),
      volume = activeFilter[Double](dailyMap, _.adjReturn, _ => 0.0),
      active = activeFilter[Boolean](dailyMap, _.active, _ => false))
  }

  def getTimeIndex(): HistoricalData = {
    // Only extracts market ticker as the main reference of market hours
    val predicate = (e: Event) => 
      (e.entityType == params.entityType && e.entityId == marketTicker)
    
    val tickerMap: Map[String, HistoricalData] = batchView
      .aggregateByEntityOrdered(
        predicate,
        YahooDataSource.Intermediate(),
        mergeTimeIndex)
      .mapValues(finalizeTimeIndex)
      
    tickerMap(marketTicker)
  }

  def getHistoricalDataSet()
  : Map[String, HistoricalData] = {
    val tickerSet = params.windowParams.tickerList.toSet
    val predicate = (e: Event) => 
      (e.entityType == params.entityType && tickerSet(e.entityId))

    tickerMap: Map[String, HistoricalData] = batchView
      .aggregateByEntityOrdered(
        predicate,
        YahooDataSource.Intermediate(),
        mergeStock)
      .mapValues(finalizeStock)
  }

  def read(sc: SparkContext)
  : Seq[(AnyRef, RDD[AnyRef], RDD[(AnyRef, AnyRef)])] = {

    val data = getHistoricalDataSet()

    data.keys.foreach { println }


    Seq[(AnyRef, RDD[AnyRef], RDD[(AnyRef, AnyRef)])]()
  }
}

object YahooDataSource {
  case class Params(
    val windowParams: DataSourceParams,
    // Below filters with DataAPISpecific details
    val appId: Int,  // Ignore appId in DataSourceParams
    val entityType: String,
    val startTime: Option[DateTime] = None,
    val untilTime: Option[DateTime] = None
  ) extends BaseParams

  case class Daily(
    val close: Double,
    val adjClose: Double,
    val adjReturn: Double,
    val volume: Double,
    val active: Boolean,
    // prevDate is used to verify continuity
    val prevDate: DateTime)

  /** Intermediate storage for constructing historical data
    * @param timeIndexSet Only datetime in this set is used to create historical
    * data.
    */
  case class Intermediate(
    val ticker: String = "", 
    //val dailyMap: MMap[DateTime, Daily] = MMap[DateTime, Daily]()
    val dailyMap: Map[DateTime, Daily] = Map[DateTime, Daily]()
    ) extends Serializable {
    override def toString(): String = 
      s"YDS.Intermediate($ticker, size=${dailyMap.size})"
  }


  /*
  def main(args: Array[String]) {
    val params = Params(
      appId = 1,
      untilTime = Some(new DateTime(2014, 5, 1, 0, 0)))
      //untilTime = None)

    val ds = new YahooDataSource(params)
    //ds.read
  }
  */
}

object YahooDataSourceRun {
  def main(args: Array[String]) {
    val dsp = YahooDataSource.Params(
      windowParams = DataSourceParams(
        baseDate = new DateTime(2014, 1, 1, 0, 0),
        fromIdx = 20,
        untilIdx = 50,
        trainingWindowSize = 15,
        maxTestingWindowSize = 10,
        marketTicker = "SPY",
        tickerList = Seq("AAPL", "MSFT", "IBM", "FB", "AMZN")),
      appId = 1,
      entityType = "yahoo",
      untilTime = Some(new DateTime(2014, 5, 1, 0, 0)))
      //untilTime = None)

    Workflow.run(
      dataSourceClassOpt = Some(classOf[YahooDataSource]),
      dataSourceParams = dsp,
      params = WorkflowParams(
        verbose = 3,
        batch = "YahooDataSource")
    )
  }
}




