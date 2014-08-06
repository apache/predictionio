package io.prediction.engines.stock2

import io.prediction.controller.Params
import io.prediction.controller.Metrics
import com.github.nscala_time.time.Imports._
import scala.collection.mutable.{ Map => MMap, ArrayBuffer }

case class BacktestingParams(
  val enterThreshold: Double,
  val exitThreshold: Double,
  val maxPositions: Int = 1)
extends Params {}

// prediction is Ticker -> ({1:Enter, -1:Exit}, ActualReturn)
class DailyResult(
  //val date: DateTime,
  val dateIdx: Int,
  val toEnter: Seq[String],
  val toExit: Seq[String])
extends Serializable {}

class BacktestingMetrics(val params: BacktestingParams)
  extends Metrics[
      BacktestingParams, DataParams, QueryDate, Prediction, AnyRef,
      DailyResult, Seq[DailyResult], String] {

  def computeUnit(queryDate: QueryDate, prediction: Prediction, 
    unusedActual: AnyRef)
    : DailyResult = {

    val todayIdx = queryDate.idx

    // Decide enter / exit, also sort by pValue desc
    val data = prediction.data
    .map { case (ticker, pValue) => {
      val dir = pValue match {
        case p if p >= params.enterThreshold => 1
        case p if p <= params.exitThreshold => -1
        case _ => 0
      }
      (ticker, dir, pValue)
    }}
    .toArray
    .sortBy(-_._3)

    val toEnter = data.filter(_._2 == 1).map(_._1)
    val toExit = data.filter(_._2 == -1).map(_._1)
    
    new DailyResult(
      dateIdx = todayIdx,
      toEnter = toEnter,
      toExit = toExit)
  }
 
  def computeSet(dp: DataParams, input: Seq[DailyResult])
    : Seq[DailyResult] = input

  def computeMultipleSets(input: Seq[(DataParams, Seq[DailyResult])])
  : String = {
    var dailyResultsSeq = input
      .map(_._2)
      .flatten
      .toArray
      .sortBy(_.dateIdx)

    var rawData = input.head._1.rawDataB.value
    val retFrame = rawData._retFrame
    val priceFrame = rawData._priceFrame

    val dailyNavs = ArrayBuffer[Double]()
    val ss = ArrayBuffer[String]()

    var cash = 1000000.0
    // Ticker to current size
    val positions = MMap[String, Double]()
    val maxPositions = params.maxPositions

    for (daily <- dailyResultsSeq) {
      val todayIdx = daily.dateIdx
      val today = rawData.timeIndex(todayIdx)
      val todayRet = retFrame.rowAt(todayIdx)

      println(today)
      println(todayRet)
      println(priceFrame.rowAt(todayIdx))

      // Update price change
      positions.keys.foreach { ticker => {
        positions(ticker) *= todayRet.first(ticker).get
      }}

      // Determine exit
      daily.toExit.foreach { ticker => {
        if (positions.contains(ticker)) {
          val money = positions.remove(ticker).get
          cash += money
          val s = s"Exit . D: $today T: $ticker M: $money"
          ss.append(s)
        }
      }}

      // Determine enter
      val slack = maxPositions - positions.size
      val money = cash / slack
      daily.toEnter
      .filter(t => !positions.contains(t))
      .take(slack)
      .map{ ticker => {
        cash -= money
        positions += (ticker -> money)
        val s = s"Enter. D: $today T: $ticker M: $money"
        ss.append(s)
      }}

      // Book keeping
      val nav = cash + positions.values.sum     
      val s = s"$today Nav: $nav Pos: ${positions.size}"
      ss.append(s)
    }
    ss.mkString("\n")
  }
}
