package io.prediction.examples.itemrank

import io.prediction.controller.Metrics

import com.github.nscala_time.time.Imports._
import scala.math.BigDecimal
import breeze.stats.{ mean, meanAndVariance }

class ItemRankMetrics(mp: MetricsParams)
  extends Metrics[MetricsParams,
    DataParams, Query, Prediction, Actual,
      MetricUnit, MetricResult, MultipleMetricResult] {

  override def computeUnit(query: Query, prediction: Prediction,
    actual: Actual): MetricUnit  = {

    val k = query.items.size

    new MetricUnit(
      q = query,
      p = prediction,
      a = actual,
      score = averagePrecisionAtK(k, prediction.items.map(_._1),
        actual.items.toSet),
      baseline = averagePrecisionAtK(k, query.items,
        actual.items.toSet))
  }

  // calcualte MAP at k
  override def computeSet(dataParams: DataParams,
    metricUnits: Seq[MetricUnit]): MetricResult = {

    val (algoMean, algoVariance, algoCount) = meanAndVariance(
      metricUnits.map(_.score)
    )
    val algoStdev = math.sqrt(algoVariance)
    val (baselineMean, baselineVariance, baselineCount) = meanAndVariance(
      metricUnits.map(_.baseline)
    )
    val baselineStdev = math.sqrt(baselineVariance)

    //val mean = metricUnits.map( eu => eu.score ).sum / metricUnits.size
    //val baseMean = metricUnits.map (eu => eu.baseline).sum /
    //  metricUnits.size

    if (mp.verbose) {
      val reports = metricUnits.map{ eu =>
        val flag = if (eu.baseline > eu.score) "x" else ""
        Seq(eu.q.uid, eu.q.items.mkString(","),
          eu.p.items.map(p => s"${p._1}(${printDouble(p._2)})").mkString(","),
          eu.a.items.mkString(","),
          printDouble(eu.baseline), printDouble(eu.score),
          flag)
      }.map { x => x.map(t => s"[${t}]")}

      println("result:")
      println(s"${dataParams.tdp.startUntil}")
      println(s"${dataParams.vdp.startUntil}")
      println("uid - basline - ranked - actual - baseline score - algo score")
      reports.foreach { r =>
        println(s"${r.mkString(" ")}")
      }
      println(s"baseline MAP@k = ${baselineMean} (${baselineStdev}), " +
        s"algo MAP@k = ${algoMean} (${algoStdev})")
    }

    new MetricResult(
      testStartUntil = dataParams.vdp.startUntil,
      baselineMean = baselineMean,
      baselineStdev = baselineStdev,
      algoMean = algoMean,
      algoStdev = algoStdev
    )
  }

  override def computeMultipleSets(
    input: Seq[(DataParams, MetricResult)]): MultipleMetricResult = {

    val mrSeq = input.map(_._2)
    // calculate mean of MAP@k of each validion result
    val (algoMean, algoVariance, algoCount) = meanAndVariance(
      mrSeq.map(_.algoMean)
    )
    val algoStdev = math.sqrt(algoVariance)

    val (baselineMean, baselineVariance, baseCount) = meanAndVariance(
      mrSeq.map(_.baselineMean)
    )
    val baselineStdev = math.sqrt(baselineVariance)

    if (mp.verbose) {
      mrSeq.sortBy(_.testStartUntil).foreach { mr =>
        println(s"${mr.testStartUntil}")
        println(s"baseline MAP@k = ${mr.baselineMean} (${mr.baselineStdev}), " +
          s"algo MAP@k = ${mr.algoMean} (${mr.algoStdev})")
      }
      println(s"baseline average = ${baselineMean} (${baselineStdev}), " +
        s"algo average = ${algoMean} (${algoStdev})")
    }

    new MultipleMetricResult (
      baselineMean = baselineMean,
      baselineStdev = baselineStdev,
      algoMean = algoMean,
      algoStdev = algoStdev
    )
  }

  private def printDouble(d: Double): String = {
    BigDecimal(d).setScale(4, BigDecimal.RoundingMode.HALF_UP).toString
  }

  // metric
  private def averagePrecisionAtK[T](k: Int, p: Seq[T], r: Set[T]): Double = {
    // supposedly the predictedItems.size should match k
    // NOTE: what if predictedItems is less than k? use the avaiable items as k.
    val n = scala.math.min(p.size, k)

    // find if each element in the predictedItems is one of the relevant items
    // if so, map to 1. else map to 0
    // (0, 1, 0, 1, 1, 0, 0)
    val rBin: Seq[Int] = p.take(n).map { x => if (r(x)) 1 else 0 }
    val pAtKNom = rBin.scanLeft(0)(_ + _)
      .drop(1) // drop 1st one which is initial 0
      .zip(rBin)
      .map(t => if (t._2 != 0) t._1.toDouble else 0.0)
    // ( number of hits at this position if hit or 0 if miss )

    val pAtKDenom = 1 to rBin.size
    val pAtK = pAtKNom.zip(pAtKDenom).map { t => t._1 / t._2 }
    val apAtKDenom = scala.math.min(n, r.size)
    if (apAtKDenom == 0) 0 else pAtK.sum / apAtKDenom
  }

}
