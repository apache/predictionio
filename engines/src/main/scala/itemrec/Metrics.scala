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

package io.prediction.engines.itemrec

import io.prediction.engines.base
import io.prediction.engines.base.BinaryRatingParams
import io.prediction.engines.base.MetricsHelper
import io.prediction.controller.Metrics
import io.prediction.controller.Params
import io.prediction.controller.NiceRendering
import io.prediction.engines.base.HasName

import io.prediction.engines.itemrank.html

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
import io.prediction.engines.base.Stats

class ItemRecMetricsParams(
  val name: String = "",
  val buckets: Int = 10,
  val ratingParams: BinaryRatingParams,
  val measureType: MeasureType.Type = MeasureType.PrecisionAtK,
  val measureK: Int = -1
) extends Params {}


object MeasureType extends Enumeration {
  type Type = Value
  val PrecisionAtK = Value
}

class MetricsUnit(
  val q: Query,
  val p: Prediction,
  val a: Actual,
  val score: Double,
  val baseline: Double,
  val uidHash: Int = 0
) extends Serializable

abstract class ItemRecMeasure extends Serializable {
  def calculate(query: Query, prediction: Prediction, actual: Actual): Double
}

class ItemRecPrecision(val k: Int, val ratingParams: BinaryRatingParams) 
  extends ItemRecMeasure {
  override def toString(): String = s"Precision@$k"
  
  def calculate(query: Query, prediction: Prediction, actual: Actual)
  : Double = {
    val kk = (if (k == -1) query.n else k)

    val actualItems = MetricsHelper.actions2GoodIids(
      actual.actionTuples, ratingParams)

    val relevantCount = prediction.items.take(kk)
      .map(_._1)
      .filter(iid => actualItems(iid))
      .size

    val denominator = Seq(kk, prediction.items.size, actualItems.size).min

    relevantCount.toDouble / denominator
  }
}

case class MetricsOutput(
  val name: String,
  val measureType: String,
  val algoMean: Double,
  val algoStats: Stats,
  //val heatMap: HeatMapData,
  val runs: Seq[(String, Stats, Stats)],  // name, algo, baseline
  val aggregations: Seq[(String, Seq[(String, Stats)])])
  extends Serializable with NiceRendering {

  override def toString(): String = {
    val b = 1.96 * algoStats.stdev / Math.sqrt(algoStats.count)
    val lb = algoStats.average - b
    val ub = algoStats.average + b
    f"$measureType $name ${algoStats.average}%.4f [$lb%.4f, $ub%.4f]"
  }

  def toHTML(): String = html.detailed().toString

  def toJSON(): String = {
    implicit val formats = DefaultFormats
    Serialization.write(this)
  }
}


class ItemRecMetrics(params: ItemRecMetricsParams) 
  extends Metrics[ItemRecMetricsParams, HasName,
      Query, Prediction, Actual,
      MetricsUnit, Seq[MetricsUnit], MetricsOutput] {
  
  val measure: ItemRecMeasure = params.measureType match {
    case MeasureType.PrecisionAtK => {
      new ItemRecPrecision(params.measureK, params.ratingParams)
    }
    case _ => {
      throw new NotImplementedError(
        s"MeasureType ${params.measureType} not implemented")
    }
  }

  
  override def computeUnit(query: Query, prediction: Prediction, actual: Actual)
  : MetricsUnit = {
    val score = measure.calculate(query, prediction, actual)

    // TODO(yipjustin): Implement baseline
    /*
    val baseline = measure.calculate(
      query,
      Prediction(query.iids.map(e => (e, 0.0)), isOriginal = false),
      actual)
    */

    new MetricsUnit(
      q = query,
      p = prediction,
      a = actual,
      score = score,
      baseline = 0,
      uidHash = MurmurHash3.stringHash(query.uid)
    )
  }
  
  override def computeSet(dataParams: HasName,
    metricUnits: Seq[MetricsUnit]): Seq[MetricsUnit] = metricUnits
  
  override def computeMultipleSets(
    rawInput: Seq[(HasName, Seq[MetricsUnit])]): MetricsOutput = {
    // Precision is undefined when the relevant items is a empty set. need to
    // filter away cases where there is no good items.
    val input = rawInput.map { case(name, mus) => {
      (name, mus.filter(mu => (!mu.score.isNaN && !mu.baseline.isNaN)))
    }}

    val allUnits: Seq[MetricsUnit] = input.flatMap(_._2)
    
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

    val outputData = MetricsOutput (
      name = params.name,
      measureType = measure.toString,
      //algoMean = 0,
      //algoStats = null,
      algoMean = overallStats._2.average,
      algoStats = overallStats._2,
      //heatMap = heatMap,
      runs = Seq(overallStats) ++ runsStats,
      //runs = Seq[(String, Stats, Stats)](),
      aggregations = Seq[(String, Seq[(String, Stats)])](
        /*
        ("By # of Rated Items", aggregateByActualSize),
        ("By # of Positively Rated Items", aggregateByActualGoodSize),
        ("By # of Items in Query", aggregateByQuerySize),
        ("By Measure Score", scoreAggregation),
        ("By IsOriginal, true when engine cannot return prediction", 
          isOriginalAggregation)
        */
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
    /*
    params.optOutputPath.map { path =>
      MV.save(outputData, path)
    }
    */

    outputData
  }

}

