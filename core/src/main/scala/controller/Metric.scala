/** Copyright 2015 TappingStone, Inc.
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

package io.prediction.controller

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._
import grizzled.slf4j.Logger

/** Base class of a [[Metric]].
  *
  * @tparam EI Evaluation information
  * @tparam Q Query
  * @tparam P Predicted result
  * @tparam A Actual result
  * @tparam R Metric result
  * @group Evaluation
  */
abstract class Metric[EI, Q, P, A, R](implicit rOrder: Ordering[R])
extends Serializable {
  /** Class name of this [[Metric]]. */
  def header: String = this.getClass.getName

  /** Calculates the result of this [[Metric]]. */
  def calculate(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])]): R

  /** Comparison function for R's ordering. */
  def compare(r0: R, r1: R): Int = rOrder.compare(r0, r1)
}

/** Returns the global average of the score returned by the calculate method.
  *
  * @tparam EI Evaluation information
  * @tparam Q Query
  * @tparam P Predicted result
  * @tparam A Actual result
  *
  * @group Evaluation
  */
abstract class AverageMetric[EI, Q, P, A]
extends Metric[EI, Q, P, A, Double] {
  /** Implement this method to return a score that will be used for averaging
    * across all QPA tuples.
    */
  def calculate(q: Q, p: P, a: A): Double

  def calculate(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])])
  : Double = {
    // TODO(yipjustin): Parallelize
    val r: Seq[(Double, Long)] = evalDataSet
    .map { case (_, qpaRDD) =>
      val s = qpaRDD.map { case (q, p, a) => calculate(q, p, a) }.reduce(_ + _)
      val c = qpaRDD.count
      (s, c)
    }

    (r.map(_._1).sum / r.map(_._2).sum)
  }
}

/** Returns the global average of the non-None score returned by the calculate
  * method.
  *
  * @tparam EI Evaluation information
  * @tparam Q Query
  * @tparam P Predicted result
  * @tparam A Actual result
  *
  * @group Evaluation
  */
abstract class OptionAverageMetric[EI, Q, P, A]
extends Metric[EI, Q, P, A, Double] {
  /** Implement this method to return a score that will be used for averaging
    * across all QPA tuples.
    */
  def calculate(q: Q, p: P, a: A): Option[Double]

  def calculate(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])])
  : Double = {
    // TODO(yipjustin): Parallelize
    val r: Seq[(Double, Long)] = evalDataSet
    .par
    .map { case (_, qpaRDD) =>
      val scores: RDD[Double]  = qpaRDD
      .map { case (q, p, a) => calculate(q, p, a) }
      .filter(!_.isEmpty)
      .map(_.get)

      // TODO: reduce and count are actions. Make them async.
      (scores.reduce(_ + _), scores.count)
    }
    .seq

    val c = r.map(_._2).sum
    if (c > 0) {
      (r.map(_._1).sum / r.map(_._2).sum)
    } else {
      Double.NegativeInfinity
    }
  }
}

