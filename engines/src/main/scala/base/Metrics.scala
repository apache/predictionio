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
      //.map{ case(k, v) => (k % params.buckets, v) }
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
}


