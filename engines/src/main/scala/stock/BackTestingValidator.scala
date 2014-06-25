package io.prediction.engines.stock

import io.prediction._
import com.github.nscala_time.time.Imports._
import io.prediction.core.BaseEvaluator
import scala.collection.mutable.{ Map => MMap, ArrayBuffer }
//import io.prediction.core.LocalEvaluator

object BackTestingEvaluator extends EvaluatorFactory {
  // Use singleton class here to avoid re-registering hooks in config.
  override def apply() = {
    new BaseEvaluator(
    //new LocalEvaluator(
      classOf[StockDataPreparator],
      classOf[BackTestingValidator])
  }
}


class BackTestingParams(
  val enterThreshold: Double,
  val exitThreshold: Double,
  val maxPositions: Int = 3)
extends BaseParams {}

// prediction is Ticker -> ({1:Enter, -1:Exit}, ActualReturn)
class DailyResults(
  val date: DateTime,
  val actualReturn: Map[String, Double],  // Tomorrow's return
  val toEnter: Seq[String],
  val toExit: Seq[String])
extends Serializable {}

class SetResults(val dailySeq: Seq[DailyResults]) 
extends Serializable {}

class BackTestingResults(val s: Seq[String]) 
extends Serializable {
  override def toString() = s.mkString("\n")
}


class BackTestingValidator
    extends Validator[
        BackTestingParams, 
        TrainingDataParams, 
        ValidationDataParams,
        Feature, 
        Target, 
        Target,
        DailyResults, 
        SetResults, 
        BackTestingResults] {
  var params: BackTestingParams = null

  override
  def init(params: BackTestingParams): Unit = {
    this.params = params
  }

  def validate(feature: Feature, predicted: Target, actual: Target)
    : DailyResults = {
    val predictedData = predicted.data
    val actualData = actual.data

    // Decide enter / exit, also sort by pValue desc
    val data = predictedData
    .map { case (ticker, pValue) => {
      val dir = pValue match {
        case p if p >= params.enterThreshold => 1
        case p if p <= params.exitThreshold => -1
        case _ => 0
      }
      (ticker, dir, pValue, actualData(ticker))
    }}
    .toArray
    .sortBy(-_._3)

    val toEnter = data.filter(_._2 == 1).map(_._1)
    val toExit = data.filter(_._2 == -1).map(_._1)
    val actualReturn = data.map(e => (e._1, e._4)).toMap
    
    new DailyResults(
      date = feature.today, 
      actualReturn = actualReturn,
      toEnter = toEnter,
      toExit = toExit)
  }
  
  def validateSet(
    trainingDataParams: TrainingDataParams,
    validationDataParams: ValidationDataParams,
    validationUnits: Seq[DailyResults])
    : SetResults = {
    new SetResults(dailySeq = validationUnits)
  }
  
  def crossValidate(
    validationResultsSeq
      : Seq[(TrainingDataParams, ValidationDataParams, SetResults)])
  : BackTestingResults = {
    var dailyResultsSeq = validationResultsSeq
      .map(_._3.dailySeq)
      .flatten
      .toArray
      .sortBy(_.date)

    val dailyNavs = ArrayBuffer[Double]()
    val ss = ArrayBuffer[String]()

    var cash = 1000000.0
    val positions = MMap[String, Double]()
    val maxPositions = params.maxPositions

    for (daily <- dailyResultsSeq) {
      val today = daily.date
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

      // Update price change
      positions.keys.foreach { ticker => {
        positions(ticker) *= (1 + daily.actualReturn(ticker))
      }}

      // Book keeping
      val nav = cash + positions.values.sum     
      val s = s"$today Nav: $nav Pos: ${positions.size}"
      ss.append(s)
    }
    new BackTestingResults(s = ss)
  }
}


