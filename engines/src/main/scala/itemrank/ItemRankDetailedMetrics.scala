package io.prediction.engines.itemrank

import io.prediction.controller.Metrics
import io.prediction.controller.Params
import io.prediction.controller.NiceRendering

import com.github.nscala_time.time.Imports._
import scala.math.BigDecimal
import breeze.stats.{ mean, meanAndVariance }

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{read, write}
import org.json4s.native.Serialization

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream

import scala.io.Source
import java.io.PrintWriter
import java.io.File

import io.prediction.engines.util.{ MetricsVisualization => MV }

case class Stats(
  val average: Double, 
  val count: Long,
  val stdev: Double,
  val min: Double,
  val max: Double
) extends Serializable

case class DetailedMetricsData(
  val name: String,
  /*
  val baselineMean: Double,
  val baselineStdev: Double,
  val algoMean: Double,
  val algoStdev: Double,
  */
  val algoMean: Double,
  val runs: Seq[(String, Stats, Stats)],  // name, algo, baseline
  val aggregations: Seq[(String, Seq[(String, Stats)])])
  extends Serializable with NiceRendering {

  override def toString(): String = f"DetailedMetricsData $name $algoMean%.4f"

  def toHTML(): String = html.detailed().toString

  def toJSON(): String = {
    implicit val formats = DefaultFormats
    Serialization.write(this)
  }
}

// optOutputPath is used for debug purpose. If specified, metrics will output
// the data class to the specified path, and the renderer can generate the html
// independently.
class DetailedMetricsParams(
  val name: String = "",
  val optOutputPath: Option[String] = None) 
  extends Params {}

class ItemRankDetailedMetrics(params: DetailedMetricsParams)
  extends Metrics[DetailedMetricsParams,
    DataParams, Query, Prediction, Actual,
      MetricUnit, Seq[MetricUnit], DetailedMetricsData] {

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
    metricUnits: Seq[MetricUnit]): Seq[MetricUnit] = metricUnits
  
  def calculate(values: Seq[Double]): Stats = {
    val (mean, variance, count) = meanAndVariance(values)
    Stats(mean, count, math.sqrt(variance), values.min, values.max)
    //Stats(values.sum / values.size, values.size)
  }

  /*
  def aggregate(
    units: Seq[MetricUnit],
    groupByFunc: MetricUnit => String): Seq[(String, Stats)] = {
    units
      .groupBy(groupByFunc)
      .mapValues(_.map(_.score))
      .map{ case(k, l) => (k, calculate(l)) }
      .toSeq
      .sortBy(-_._2.average)
  } 
  */

  def aggregateMU(units: Seq[MetricUnit], groupByFunc: MetricUnit => String)
  : Seq[(String, Stats)] = 
    aggregate[MetricUnit](units, _.score, groupByFunc)

  def aggregate[T](
    units: Seq[T],
    scoreFunc: T => Double,
    groupByFunc: T => String): Seq[(String, Stats)] = {
    units
      .groupBy(groupByFunc)
      .mapValues(_.map(e => scoreFunc(e)))
      .map{ case(k, l) => (k, calculate(l)) }
      .toSeq
      .sortBy(-_._2.average)
  } 

  // return a double to key map based on boundaries.
  def groupByRange(values: Array[Double], format: String = "%f")
  : Double => String = {
    val keys: Array[String] = (0 to values.size).map { i =>
      val s = (if (i == 0) Double.NegativeInfinity else values(i-1))
      val e = (if (i < values.size) values(i) else Double.PositiveInfinity)
      //s"[$s, $e)"
      "[" + format.format(s) + ", " + format.format(e) + ")"
    }.toArray
  
    def f(v: Double): String = {
      // FIXME. Use binary search.
      val i: Option[Int] = (0 until values.size).find(i => v < values(i))
      keys(i.getOrElse(values.size))
    }
    return f
  }

  override def computeMultipleSets(
    input: Seq[(DataParams, Seq[MetricUnit])]): DetailedMetricsData = {
    val allUnits: Seq[MetricUnit] = input.flatMap(_._2) 

    //val overallStats = ("Overall", calculate(allUnits.map(_.score)))
    //val baselineStats = ("Baseline", calculate(allUnits.map(_.baseline)))

    // Run Stats
    val overallStats = (
      "Overall", 
      calculate(allUnits.map(_.score)),
      calculate(allUnits.map(_.baseline)))

    val runsStats: Seq[(String, Stats, Stats)] = input
    .map { case(dp, mus) => 
      (dp.name, calculate(mus.map(_.score)), calculate(mus.map(_.baseline)))
    }

    // Aggregation Stats
    val aggregateByActualSize: Seq[(String, Stats)] = allUnits
      .groupBy(_.a.items.size)
      .mapValues(_.map(_.score))
      .map{ case(k, l) => (k.toString, calculate(l)) }
      .toSeq
      .sortBy(-_._2.average)

    val scoreAggregation = aggregateMU(
      allUnits,
      mu => groupByRange((0.0 until 1.0 by 0.1).toArray, "%.2f")(mu.score))

    val actionCountAggregation = aggregateMU(
      allUnits, 
      mu => groupByRange(Array(0, 1, 3, 10, 30, 100, 300), "%.0f")
        (mu.a.previousActionCount))

    val itemCountAggregation = aggregate[(String, MetricUnit)](
      allUnits.flatMap(mu => mu.a.items.map(item => (item, mu))),
      _._2.score,
      _._1)

    val dateAggregation = aggregateMU(
      allUnits,
      _.a.localDate.toString)

    val localHourAggregation = aggregateMU(
      allUnits,
      _.a.localDateTime.getHourOfDay.toString)

    val isOriginalAggregation = aggregateMU(allUnits, _.p.isOriginal.toString)

    val avgOrderSizeAggregation = aggregateMU(
      allUnits,
      mu => groupByRange(Array(0, 1, 2, 3, 5, 8, 13), "%.0f")
        (mu.a.averageOrderSize))
    
    val previousOrdersAggregation = aggregateMU(
      allUnits,
      mu => groupByRange(Array(0, 1, 3, 10, 30, 100), "%.0f")
        (mu.a.previousOrders))
      
    val outputData = DetailedMetricsData (
      name = params.name,
      algoMean = overallStats._2.average,
      //runs = Seq(overallStats, baselineStats) ++ runsStats,
      runs = Seq(overallStats) ++ runsStats,
      aggregations = Seq(
        ("ByActualSize", aggregateMU(allUnits, _.a.items.size.toString)),
        ("ByScore", scoreAggregation),
        ("ByActionCount", actionCountAggregation),
        ("ByFlattenItem", itemCountAggregation),
        ("ByDate", dateAggregation),
        ("ByLocalHour", localHourAggregation),
        ("ByIsOriginal", isOriginalAggregation),
        ("ByAvgOrderSize", avgOrderSizeAggregation),
        ("ByPreviousOrders", previousOrdersAggregation)
      )
    )

    // FIXME: Use param opt path
    params.optOutputPath.map { path => 
      MV.save(outputData, path)
    }

    outputData
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

object ItemRankDetailedMain {
  def main(args: Array[String]) {
    MV.render(MV.load[DetailedMetricsData](args(0)), args(0))
  }
}


