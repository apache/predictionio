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

package io.prediction.engines.base

import io.prediction.controller.Params
import io.prediction.controller.Metrics
import breeze.stats.{ mean, meanAndVariance }
import io.prediction.controller.NiceRendering

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{read, write}
import org.json4s.native.Serialization

trait HasName {
  def name: String
}

case class Stats(
  val average: Double,
  val count: Long,
  val stdev: Double,
  val min: Double,
  val max: Double
) extends Serializable

// Describe how to map from actions to a binary rating parameter
class BinaryRatingParams(
  val actionsMap: Map[String, Option[Int]], // ((view, 1), (rate, None))
  val goodThreshold: Int  // action rating >= goodThreshold is good.
) extends Serializable

// This class is used with the html rendering class. See toHTML()
case class MetricsOutput(
  val name: String,
  val metricsName: String,
  val description: String = "",
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


object MetricsHelper {
  def actions2GoodIids(
    actionTuples: Seq[(String, String, U2IActionTD)], 
    binaryRatingParams: BinaryRatingParams)
  : Set[String] = {
    actionTuples
    .filter{ t => {
      val u2i = t._3
      val rating = Preparator.action2rating(
        u2i = u2i,
        actionsMap = binaryRatingParams.actionsMap,
        default = 0)
      rating >= binaryRatingParams.goodThreshold
    }}
    .map(_._2)
    .toSet
  }
  
  def calculateResample(values: Seq[(Int, Double)], buckets: Int): Stats = {
    if (values.isEmpty) {
      return Stats(0, 0, 0, 0, 0)
    }

    // JackKnife resampling.
    val segmentSumCountMap: Map[Int, (Double, Int)] = values
      .map{ case(k, v) => (k % buckets, v) }
      .groupBy(_._1)
      .mapValues(l => (l.map(_._2).sum, l.size))

    val sum = values.map(_._2).sum
    val count = values.size

    val segmentMeanMap: Map[Int, Double] = segmentSumCountMap
      .mapValues { case(sSum, sCount) => (sum - sSum) / (count - sCount) }

    val segmentValues = segmentMeanMap.values

    // Assume the mean is normally distributed
    val mvc = meanAndVariance(segmentValues)

    // Stats describes the properties of the buckets
    return Stats(mvc.mean, buckets, mvc.stdDev, 
      segmentValues.min, segmentValues.max)
  }
  
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
  
  def calculate(values: Seq[Double]): Stats = {
    val mvc = meanAndVariance(values)
    Stats(mvc.mean, mvc.count, mvc.stdDev, values.min, values.max)
  }
  
  // return a double to key map based on boundaries.
  def groupByRange(values: Array[Double], format: String = "%f")
  : Double => String = {
    val keys: Array[String] = (0 to values.size).map { i =>
      val s = (if (i == 0) Double.NegativeInfinity else values(i-1))
      val e = (if (i < values.size) values(i) else Double.PositiveInfinity)
      "[" + format.format(s) + ", " + format.format(e) + ")"
    }.toArray

    def f(v: Double): String = {
      // FIXME. Use binary search or indexWhere
      val i: Option[Int] = (0 until values.size).find(i => v < values(i))
      keys(i.getOrElse(values.size))
    }
    return f
  }
}


