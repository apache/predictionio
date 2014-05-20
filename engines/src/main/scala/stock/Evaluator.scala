package io.prediction.engines.stock

import io.prediction.Evaluator
import scala.math
import org.saddle._
import org.saddle.index.IndexTime

import scala.collection.mutable.{ Map => MMap }
import com.github.nscala_time.time.Imports._

import breeze.linalg.{ DenseMatrix, DenseVector }
import breeze.stats.{ mean, meanAndVariance }
import nak.regress.LinearRegression
import scala.collection.mutable.ArrayBuffer

class StockEvaluator extends Evaluator[EvaluationParams, TrainingDataParams, EvaluationDataParams, Feature, Target] {
  // (predicted, acutal)
  val results = ArrayBuffer[(Double, Double)]()

  def getParamsSet(params: EvaluationParams): Seq[(TrainingDataParams, EvaluationDataParams)] = {
    Range(params.fromIdx, params.untilIdx, params.evaluationInterval)
      .map(idx => {
        val trainParams = new TrainingDataParams(
          baseDate = params.baseDate,
          untilIdx = idx - 1,
          windowSize = params.trainingWindowSize,
          marketTicker = params.marketTicker,
          tickerList = params.tickerList)
        val evalParams = new EvaluationDataParams(
          baseDate = params.baseDate,
          fromIdx = idx,
          untilIdx = math.min(
            idx + params.evaluationInterval,
            params.untilIdx),
          marketTicker = params.marketTicker,
          tickerList = params.tickerList)
        (trainParams, evalParams)
      })
  }

  def evaluate(feature: Feature, predicted: Target, actual: Target) {
    val predictedData = predicted.data
    val actualData = actual.data

    predictedData.foreach {
      case (ticker, pValue) => {
        results.append((pValue, actualData(ticker)))
      }
    }
  }

  def report() {
    val pThresholds = Seq(-0.01, -0.003, -0.001, -0.0003,
      0.0, 0.0003, 0.001, 0.003, 0.01)

    for (pThreshold <- pThresholds) {
      val screened = results.filter(e => e._1 > pThreshold).toSeq
      val over = screened.filter(e => (e._1 > e._2)).length
      val under = screened.filter(e => (e._1 < e._2)).length
      // Sum actual return.
      val actuals = screened.map(_._2)
      val (mean_, variance, count) = meanAndVariance(actuals)
      val stdev = math.sqrt(variance)
      // 95% CI
      val ci = 1.96 * stdev / math.sqrt(count)

      println(f"Threshold: ${pThreshold}%+.4f " +
        f"Mean: ${mean_}%+.6f Stdev: ${stdev}%.6f CI: ${ci}%.6f " +
        f"Total: ${count}%5d Over: $over%5d Under: $under%5d")
    }
  }
}
