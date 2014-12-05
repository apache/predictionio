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
//import io.prediction.engines.base.html
import io.prediction.engines.base.BinaryRatingParams
import io.prediction.engines.base.EvaluatorHelper
import io.prediction.engines.base.EvaluatorOutput
import io.prediction.controller.Evaluator
import io.prediction.controller.Params
import io.prediction.engines.base.HasName

//import io.prediction.engines.itemrank.html

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

import io.prediction.engines.util.{ EvaluatorVisualization => MV }
import scala.util.hashing.MurmurHash3
import scala.collection.immutable.NumericRange
import scala.collection.immutable.Range
import io.prediction.engines.base.Stats

import scala.util.Random
import scala.collection.mutable.{ HashSet => MHashSet }
import scala.collection.immutable.HashSet

class ItemRecEvaluatorParams(
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

class EvaluatorUnit(
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

    val actualItems = EvaluatorHelper.actions2GoodIids(
      actual.actionTuples, ratingParams)

    val relevantCount = prediction.items.take(kk)
      .map(_._1)
      .filter(iid => actualItems(iid))
      .size

    val denominator = Seq(kk, prediction.items.size, actualItems.size).min

    relevantCount.toDouble / denominator
  }
}


class ItemRecEvaluator(params: ItemRecEvaluatorParams) 
  extends Evaluator[HasName,
      Query, Prediction, Actual,
      EvaluatorUnit, Seq[EvaluatorUnit], EvaluatorOutput] {
  
  val measure: ItemRecMeasure = params.measureType match {
    case MeasureType.PrecisionAtK => {
      new ItemRecPrecision(params.measureK, params.ratingParams)
    }
    case _ => {
      throw new NotImplementedError(
        s"MeasureType ${params.measureType} not implemented")
    }
  }

  
  override def evaluateUnit(query: Query, prediction: Prediction, actual: Actual)
  : EvaluatorUnit = {
    val score = measure.calculate(query, prediction, actual)

    val uidHash: Int = MurmurHash3.stringHash(query.uid)

    // Create a baseline by randomly picking query.n items from served items.
    
    val rand = new Random(seed = uidHash)
    //val iidSet = HashSet[String]()

    val servedIids = actual.servedIids

    //val n = math.min(query.n, servedIids.size)

    val randIids = rand.shuffle(
      (if (query.n > servedIids.size) {
          // If too few items where served, use only the served items
          servedIids.toSet
        } else {
          val tempSet = MHashSet[String]()
          while (tempSet.size < query.n) {
            tempSet.add(servedIids(rand.nextInt(query.n)))
          }
          tempSet.toSet
        }
      )
      .toSeq
      .sorted
    )

    println(randIids.mkString("Randiid: [", ", ", "]"))

    // TODO(yipjustin): Implement baseline
    val baseline = measure.calculate(
      query,
      Prediction(randIids.map(iid => (iid, 0.0))),
      actual)

    new EvaluatorUnit(
      q = query,
      p = prediction,
      a = actual,
      score = score,
      baseline = baseline,
      uidHash = uidHash
    )
  }
  
  override def evaluateSet(dataParams: HasName,
    metricUnits: Seq[EvaluatorUnit]): Seq[EvaluatorUnit] = metricUnits
  
  override def evaluateAll(
    rawInput: Seq[(HasName, Seq[EvaluatorUnit])]): EvaluatorOutput = {
    // Precision is undefined when the relevant items is a empty set. need to
    // filter away cases where there is no good items.
    val input = rawInput.map { case(name, mus) => {
      (name, mus.filter(mu => (!mu.score.isNaN && !mu.baseline.isNaN)))
    }}

    val allUnits: Seq[EvaluatorUnit] = input.flatMap(_._2)
    
    val overallStats = (
      "Overall",
      EvaluatorHelper.calculateResample(
        values = allUnits.map(mu => (mu.uidHash, mu.score)),
        buckets = params.buckets),
      EvaluatorHelper.calculateResample(
        values = allUnits.map(mu => (mu.uidHash, mu.baseline)),
        buckets = params.buckets)
      )

    val runsStats: Seq[(String, Stats, Stats)] = input
    .map { case(dp, mus) =>
      (dp.name,
        EvaluatorHelper.calculateResample(
          values = mus.map(mu => (mu.uidHash, mu.score)),
          buckets = params.buckets),
        EvaluatorHelper.calculateResample(
          values = mus.map(mu => (mu.uidHash, mu.baseline)),
          buckets = params.buckets))
    }
    .sortBy(_._1)
    
    val aggregateByActualGoodSize = aggregateMU(
      allUnits,
      mu => EvaluatorHelper.groupByRange(
        Array(0, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144), "%.0f")(
          base.EvaluatorHelper.actions2GoodIids(mu.a.actionTuples, params.ratingParams).size))

    val outputData = EvaluatorOutput (
      name = params.name,
      evaluatorName = "ItemRecEvaluator",
      measureType = measure.toString,
      algoMean = overallStats._2.average,
      algoStats = overallStats._2,
      //heatMap = heatMap,
      runs = Seq(overallStats) ++ runsStats,
      aggregations = Seq[(String, Seq[(String, Stats)])](
        ("By # of Positively Rated Items", aggregateByActualGoodSize)
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

  def aggregateMU(units: Seq[EvaluatorUnit], groupByFunc: EvaluatorUnit => String)
  : Seq[(String, Stats)] = {
    EvaluatorHelper.aggregate[EvaluatorUnit](units, _.score, groupByFunc)
  }
}

