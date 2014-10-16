/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.engines.itemrank

import io.prediction.engines.base
import io.prediction.engines.base.Stats
import io.prediction.engines.base.HasName
import io.prediction.engines.base.MetricsHelper
import io.prediction.engines.base.MetricsOutput
import io.prediction.engines.base.DataParams
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
import scala.util.hashing.MurmurHash3
import scala.collection.immutable.NumericRange
import scala.collection.immutable.Range


case class HeatMapData (
  val columns: Array[String],
  val data: Seq[(String, Array[Double])]
) extends Serializable

class MetricUnit(
  val q: Query,
  val p: Prediction,
  val a: Actual,
  val score: Double,
  val baseline: Double,
  // a hashing function to bucketing uids
  val uidHash: Int = 0
) extends Serializable


object MeasureType extends Enumeration {
  type Type = Value
  val MeanAveragePrecisionAtK, PrecisionAtK = Value
}

abstract class ItemRankMeasure extends Serializable {
  def calculate(query: Query, prediction: Prediction, actual: Actual): Double
}

// If k == -1, use query size. Otherwise, use k.
class ItemRankMAP(val k: Int, val metricsParams: DetailedMetricsParams) 
  extends ItemRankMeasure {
  def calculate(query: Query, prediction: Prediction, actual: Actual)
  : Double = {
    val kk = (if (k == -1) query.iids.size else k)

    val goodIids = base.MetricsHelper.actions2GoodIids(
      actual.actionTuples, metricsParams.ratingParams)

    averagePrecisionAtK(
      kk,
      prediction.items.map(_._1),
      goodIids)
      //actual.iids.toSet)
  }

  override def toString(): String = s"MAP@$k"

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

class ItemRankPrecision(val k: Int, val metricsParams: DetailedMetricsParams) 
  extends ItemRankMeasure {
  override def toString(): String = s"Precision@$k"

  def calculate(query: Query, prediction: Prediction, actual: Actual)
  : Double = {
    val kk = (if (k == -1) query.iids.size else k)

    val actualItems = base.MetricsHelper.actions2GoodIids(
      actual.actionTuples, metricsParams.ratingParams)

    val relevantCount = prediction.items.take(kk)
      .map(_._1)
      .filter(iid => actualItems(iid))
      .size

    val denominator = Seq(kk, prediction.items.size, actualItems.size).min

    relevantCount.toDouble / denominator
  }
}

// optOutputPath is used for debug purpose. If specified, metrics will output
// the data class to the specified path, and the renderer can generate the html
// independently.
class DetailedMetricsParams(
  val name: String = "",
  val ratingParams: base.BinaryRatingParams,
  val optOutputPath: Option[String] = None,
  val buckets: Int = 10,
  val measureType: MeasureType.Type = MeasureType.MeanAveragePrecisionAtK,
  val measureK: Int = -1
) extends Params {}

class ItemRankDetailedMetrics(params: DetailedMetricsParams)
  extends Metrics[DetailedMetricsParams,
    HasName, Query, Prediction, Actual,
      MetricUnit, Seq[MetricUnit], MetricsOutput] {

  val measure: ItemRankMeasure = params.measureType match {
    case MeasureType.MeanAveragePrecisionAtK => {
      new ItemRankMAP(params.measureK, params)
    }
    case MeasureType.PrecisionAtK => {
      new ItemRankPrecision(params.measureK, params)
    }
    case _ => {
      throw new NotImplementedError(
        s"MeasureType ${params.measureType} not implemented")
    }
  }

  override def computeUnit(query: Query, prediction: Prediction,
    actual: Actual): MetricUnit  = {

    val score = measure.calculate(query, prediction, actual)
    // For calculating baseline, we use the input order of query.
    val baseline = measure.calculate(
      query,
      Prediction(query.iids.map(e => (e, 0.0)), isOriginal = false),
      actual)

    new MetricUnit(
      q = query,
      p = prediction,
      a = actual,
      score = score,
      baseline = baseline,
      uidHash = MurmurHash3.stringHash(query.uid)
    )
  }

  // calcualte MAP at k
  override def computeSet(dataParams: HasName,
    metricUnits: Seq[MetricUnit]): Seq[MetricUnit] = metricUnits

  def aggregateMU(units: Seq[MetricUnit], groupByFunc: MetricUnit => String)
  : Seq[(String, Stats)] = {
    //aggregate[MetricUnit](units, _.score, groupByFunc)
    MetricsHelper.aggregate[MetricUnit](units, _.score, groupByFunc)
  }

  def computeHeatMap(
    input: Seq[(String, Seq[Double])],
    boundaries: Array[Double],
    format: String = "%.2f") : HeatMapData = {

    val bCount: Int = boundaries.size

    val columns: Array[String] = (0 to bCount).map { i =>
      val s = (if (i == 0) Double.NegativeInfinity else boundaries(i-1))
      val e = (
        if (i < boundaries.size) boundaries(i)
        else Double.PositiveInfinity)
      "[" + format.format(s) + ", " + format.format(e) + ")"
    }.toArray

    val data: Seq[(String, Array[Double])] = input
    .map { case(key, values) => {
      val sum = values.size
      val distributions: Map[Int, Double] = values
        .map { v =>
          val i = boundaries.indexWhere(b => (v <= b))
          (if (i == -1) bCount else i)
        }
        .groupBy(identity)
        .mapValues(_.size.toDouble)
        .mapValues(v => v / sum)

      (key, (0 to bCount).map { i => distributions.getOrElse(i, 0.0) }.toArray)
    }}

    HeatMapData(columns = columns, data = data)
  }

  override def computeMultipleSets(
    rawInput: Seq[(HasName, Seq[MetricUnit])]): MetricsOutput = {
    // Precision is undefined when the relevant items is a empty set. need to
    // filter away cases where there is no good items.
    val input = rawInput.map { case(name, mus) => {
      (name, mus.filter(mu => (!mu.score.isNaN && !mu.baseline.isNaN)))
    }}

    val allUnits: Seq[MetricUnit] = input.flatMap(_._2)

    val overallStats = (
      "Overall",
      MetricsHelper.calculateResample(
        values = allUnits.map(mu => (mu.uidHash, mu.score)),
        buckets = params.buckets),
      MetricsHelper.calculateResample(
        values = allUnits.map(mu => (mu.uidHash, mu.baseline)),
        buckets = params.buckets)
      )

    val runsStats: Seq[(String, Stats, Stats)] = input
    .map { case(dp, mus) =>
      (dp.name,
        MetricsHelper.calculateResample(
          values = mus.map(mu => (mu.uidHash, mu.score)),
          buckets = params.buckets),
        MetricsHelper.calculateResample(
          values = mus.map(mu => (mu.uidHash, mu.baseline)),
          buckets = params.buckets))
    }
    .sortBy(_._1)

    // Aggregation Stats
    val aggregateByActualSize = aggregateMU(
      allUnits,
      mu => MetricsHelper.groupByRange(
        Array(0, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144), "%.0f")(
          mu.a.actionTuples.size))
    
    val aggregateByActualGoodSize = aggregateMU(
      allUnits,
      mu => MetricsHelper.groupByRange(
        Array(0, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144), "%.0f")(
          base.MetricsHelper.actions2GoodIids(mu.a.actionTuples, params.ratingParams).size))
    
    val aggregateByQuerySize = aggregateMU(
      allUnits,
      mu => MetricsHelper.groupByRange(
        Array(0, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144), "%.0f")(
          mu.q.iids.size))

    val scoreAggregation = aggregateMU(
      allUnits,
      mu => MetricsHelper.groupByRange((0.0 until 1.0 by 0.1).toArray, "%.2f")(mu.score))

    val actionCountAggregation = aggregateMU(
      allUnits,
      mu => MetricsHelper.groupByRange(Array(0, 1, 3, 10, 30, 100, 300), "%.0f")
        (mu.a.previousActionCount))

    val itemCountAggregation = MetricsHelper.aggregate[(String, MetricUnit)](
      allUnits.flatMap(mu => mu.a.actionTuples.map(t => (t._2, mu))),
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
      mu => MetricsHelper.groupByRange(Array(0, 1, 2, 3, 5, 8, 13), "%.0f")
        (mu.a.averageOrderSize))

    val previousOrdersAggregation = aggregateMU(
      allUnits,
      mu => MetricsHelper.groupByRange(Array(0, 1, 3, 10, 30, 100), "%.0f")
        (mu.a.previousOrders))

    val varietyAggregation = aggregateMU(
      allUnits,
      mu => MetricsHelper.groupByRange(Array(0, 1, 2, 3, 5, 8, 13, 21), "%.0f")
        (mu.a.variety))

    val heatMapInput: Seq[(String, Seq[Double])] = input
      .map { case(k, mus) => (k.name, mus.map(_.score)) }

    val heatMap: HeatMapData = computeHeatMap(
      heatMapInput,
      (0.0 until 1.0 by 0.0333).toArray,
      "%.2f")

    val outputData = MetricsOutput (
      name = params.name,
      metricsName = "ItemRankMetrics",
      description = "",
      measureType = measure.toString,
      algoMean = overallStats._2.average,
      algoStats = overallStats._2,
      //heatMap = heatMap,
      runs = Seq(overallStats) ++ runsStats,
      aggregations = Seq(
        ("By # of Rated Items", aggregateByActualSize),
        ("By # of Positively Rated Items", aggregateByActualGoodSize),
        ("By # of Items in Query", aggregateByQuerySize),
        ("By Measure Score", scoreAggregation),
        ("By IsOriginal, true when engine cannot return prediction", 
          isOriginalAggregation)
        /*
        ("ByActionCount", actionCountAggregation),
        ("ByFlattenItem", itemCountAggregation),
        ("ByDate", dateAggregation),
        ("ByLocalHour", localHourAggregation),
        ("ByAvgOrderSize", avgOrderSizeAggregation),
        ("ByPreviousOrders", previousOrdersAggregation),
        ("ByVariety", varietyAggregation)
        */
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
}

object ItemRankDetailedMain {
  def main(args: Array[String]) {
    MV.render(MV.load[MetricsOutput](args(0)), args(0))
  }
}
