package io.prediction.engines.stock

import io.prediction.storage.Config
import io.prediction.storage.{ ItemTrend, ItemTrends }
import io.prediction.PIOSettings

import io.prediction.DataPreparator
import io.prediction.Validator
import io.prediction.EvaluatorFactory
import io.prediction.core.AbstractEvaluator
import io.prediction.core.BaseEvaluator
import io.prediction.BaseValidationParams
import io.prediction.BaseValidationResults
import io.prediction.BaseCrossValidationResults
import io.prediction.EmptyParams

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

object StockEvaluator extends EvaluatorFactory {
  // Use singleton class here to avoid re-registering hooks in config.
  val config = new Config()
  val itemTrendsDb = config.getAppdataItemTrends()

  override def apply()
  : BaseEvaluator[
      EvaluationDataParams,
      EmptyParams,
      TrainingDataParams,
      ValidationDataParams,
      TrainingData,
      Feature,
      Target,
      Target,
      ValidationUnit, 
      ValidationResults, 
      BaseCrossValidationResults] = {
    //new StockEvaluator
    new BaseEvaluator(
      classOf[StockDataPreparator],
      classOf[StockValidator])
  }
}

object LocalFileStockEvaluator extends EvaluatorFactory {
  override def apply(): AbstractEvaluator = {
    //new StockEvaluator
    new BaseEvaluator(
      classOf[LocalFileStockDataPreparator],
      classOf[StockValidator])
  }
}

class LocalFileStockDataPreparator
    extends DataPreparator[
        EvaluationDataParams, 
        TrainingDataParams, 
        ValidationDataParams,
        TrainingData, 
        Feature, 
        Target] {
  def getParamsSet(params: EvaluationDataParams)
  : Seq[(TrainingDataParams, ValidationDataParams)] = {
    Range(params.fromIdx, params.untilIdx, params.evaluationInterval)
      .map(idx => {
        val trainParams = new TrainingDataParams(
          baseDate = params.baseDate,
          untilIdx = idx - 1,
          windowSize = params.trainingWindowSize,
          marketTicker = params.marketTicker,
          tickerList = params.tickerList)
        val validationParams = new ValidationDataParams(
          baseDate = params.baseDate,
          fromIdx = idx,
          untilIdx = math.min(
            idx + params.evaluationInterval,
            params.untilIdx),
          marketTicker = params.marketTicker,
          tickerList = params.tickerList)
        (trainParams, validationParams)
      })
  }

  def prepareTraining(params: TrainingDataParams): TrainingData = {
    null
  }

  def prepareValidation(params: ValidationDataParams)
  : Seq[(Feature, Target)] = {
    Seq[(Feature, Target)]()
  }
}

class StockDataPreparator
    extends DataPreparator[
        EvaluationDataParams, 
        TrainingDataParams, 
        ValidationDataParams,
        TrainingData, 
        Feature, 
        Target] {
  val appid = PIOSettings.appid
  val itemTrendsDbGetTicker = StockEvaluator.itemTrendsDb.get(appid, _: String).get

  // (predicted, acutal)
  def getParamsSet(params: EvaluationDataParams)
  : Seq[(TrainingDataParams, ValidationDataParams)] = {
    Range(params.fromIdx, params.untilIdx, params.evaluationInterval)
      .map(idx => {
        val trainParams = new TrainingDataParams(
          baseDate = params.baseDate,
          untilIdx = idx - 1,
          windowSize = params.trainingWindowSize,
          marketTicker = params.marketTicker,
          tickerList = params.tickerList)
        val validationParams = new ValidationDataParams(
          baseDate = params.baseDate,
          fromIdx = idx,
          untilIdx = math.min(
            idx + params.evaluationInterval,
            params.untilIdx),
          marketTicker = params.marketTicker,
          tickerList = params.tickerList)
        (trainParams, validationParams)
      })
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
      .filter { case (ticker, optData) => !optData.isEmpty }
      .map { case (ticker, optData) => (ticker, optData.get) }
    //new TrainingData(price = Frame(tickerDataSeq: _*))
    new TrainingData(boxedPrice = MeatLocker(Frame(tickerDataSeq: _*)))
  }

  // Generate evaluation data set with target data up to idx (exclusive)
  // e.g. idx = 5, window_size = 3, data = [3,4,5,6,7,8,9,10,11,12]
  // The data visible to this function should be everything up to idx = 5:
  //   visible_data = [3,4,5,6,7]
  //   feature use [4,5,6]
  //   target use [6,7], (as target represents daily return)
  def prepareOneValidation(idx: Int,
    baseDate: DateTime,
    marketTicker: String, tickerList: Seq[String]): (Feature, Target) = {
    val featureWindowSize = 60 // engine-specific param

    val featureFromIdx = idx - 1 - featureWindowSize
    val featureUntilIdx = idx - 1

    val timeIndex = getTimeIndex(baseDate, marketTicker)
      .slice(featureFromIdx, idx)

    // generate (ticker, feature, target)-tuples
    val data = tickerList
      .map(ticker => (ticker, getData(timeIndex, ticker)))
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
    //return (new Feature(data = featureData), new Target(data = targetData))
    return (new Feature(
      boxedData = MeatLocker(featureData)), 
      new Target(data = targetData))
  }

  def prepareValidation(params: ValidationDataParams)
  : Seq[(Feature, Target)] = {
    (params.fromIdx until params.untilIdx).map(idx =>
      prepareOneValidation(idx, params.baseDate,
        params.marketTicker, params.tickerList)
    ).toSeq
  }
}


class StockValidator
    extends Validator[
        EmptyParams, 
        TrainingDataParams, 
        ValidationDataParams,
        Feature, 
        Target, 
        Target,
        ValidationUnit, 
        ValidationResults, 
        BaseCrossValidationResults] {
  def validate(feature: Feature, predicted: Target, actual: Target)
      : ValidationUnit = {
    val predictedData = predicted.data
    val actualData = actual.data

    val data = predictedData.map {
      case (ticker, pValue) => {
        (pValue, actualData(ticker))
      }
    }.toSeq
    new ValidationUnit(data = data)
  }

  override 
  def validateSet(
    trainingDataParams: TrainingDataParams,
    validationDataParams: ValidationDataParams,
    validationUnits: Seq[ValidationUnit])
    : ValidationResults = {
    val results: Seq[(Double, Double)] = validationUnits.map(_.data).flatten
    val pThresholds = Seq(-0.01, -0.003, -0.001, -0.0003,
      0.0, 0.0003, 0.001, 0.003, 0.01)

    val output = pThresholds.map( pThreshold => {
      val screened = results.filter(e => e._1 > pThreshold).toSeq
      val over = screened.filter(e => (e._1 > e._2)).length
      val under = screened.filter(e => (e._1 < e._2)).length
      // Sum actual return.
      val actuals = screened.map(_._2)
      val (mean_, variance, count) = meanAndVariance(actuals)
      val stdev = math.sqrt(variance)
      // 95% CI
      val ci = 1.96 * stdev / math.sqrt(count)

      val s = (f"Threshold: ${pThreshold}%+.4f " +
        f"Mean: ${mean_}%+.6f Stdev: ${stdev}%.6f CI: ${ci}%.6f " +
        f"Total: ${count}%5d Over: $over%5d Under: $under%5d")

      println(s)
      s
    })
    new ValidationResults(data = output)
  }

  override
  def crossValidate(
    validateResultsSeq
      : Seq[(TrainingDataParams, ValidationDataParams, ValidationResults)])
  : BaseCrossValidationResults = {
    println("Cross Validation")
    validateResultsSeq.map(e => e._3.data.map(println))
    null
  }
}
