package io.prediction.examples.stock

import io.prediction.controller.Metrics
import io.prediction.controller.EmptyParams
import breeze.stats.{ mean, meanAndVariance, MeanAndVariance }

// [[[DailyMetrics]]] aggregate the overall return by the strategy.
class DailyMetrics
  extends Metrics[EmptyParams, AnyRef, Query, Target, Target,
      Seq[(Double, Double)], Seq[(Double, Double)], String] {

  def computeUnit(query: Query, predicted: Target, actual: Target)
    : Seq[(Double, Double)] = {
    val predictedData = predicted.data
    val actualData = actual.data

    predictedData.map {
      case (ticker, pValue) => {
        (pValue, actualData(ticker))
      }
    }.toSeq
  }

  def computeSet(param: AnyRef, input: Seq[Seq[(Double, Double)]])
    : Seq[(Double, Double)] = {
    input.flatten
  }

  def computeMultipleSets(input: Seq[(AnyRef, Seq[(Double, Double)])])
    : String = {
    val results: Seq[(Double, Double)] = input.map(_._2).flatten

    val pThresholds = Seq(-0.01, -0.003, -0.001, -0.0003,
      0.0, 0.0003, 0.001, 0.003, 0.01)

    val output = pThresholds.map { pThreshold => {
      val screened = results.filter(e => e._1 > pThreshold).toSeq
      val over = screened.filter(e => (e._1 > e._2)).length
      val under = screened.filter(e => (e._1 < e._2)).length
      // Sum actual return.
      val actuals = screened.map(_._2)
      //val (mean_, variance, count) = meanAndVariance(actuals)
      //val stdev = math.sqrt(variance)
      val stats = meanAndVariance(actuals)
      // 95% CI
      val ci = 1.96 * stats.stdDev / math.sqrt(stats.count)

      val s = (f"Threshold: ${pThreshold}%+.4f " +
        f"Mean: ${stats.mean}%+.6f Stdev: ${stats.stdDev}%.6f CI: ${ci}%.6f " +
        f"Total: ${stats.count}%5d Over: $over%5d Under: $under%5d")

      //println(s)
      s
    }}
    output.mkString("\n")
  }
}
    
