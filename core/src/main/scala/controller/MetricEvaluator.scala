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

import com.github.nscala_time.time.Imports.DateTime
import com.twitter.chill.KryoInjection
import grizzled.slf4j.{ Logger, Logging }
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseDataSource
import io.prediction.core.BaseEngine
import io.prediction.core.BaseEvaluator
import io.prediction.core.BasePreparator
import io.prediction.core.BaseServing
import io.prediction.core.Doer
import io.prediction.data.storage.EngineInstance
import io.prediction.data.storage.EngineInstances
import io.prediction.data.storage.Model
import io.prediction.data.storage.Storage
import io.prediction.workflow.NameParamsSerializer
import _root_.java.io.FileInputStream
import _root_.java.io.FileOutputStream
import _root_.java.io.ObjectInputStream
import _root_.java.io.ObjectOutputStream
import _root_.java.lang.{ Iterable => JIterable }
import _root_.java.util.{ HashMap => JHashMap, Map => JMap }
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.json4s._
import org.json4s.native.Serialization.write
import org.json4s.native.Serialization.writePretty
import scala.collection.JavaConversions._
import scala.language.existentials
import scala.reflect.ClassTag
import scala.reflect.Manifest

case class MetricEvaluatorResult[R](
  val bestScore: R,
  val bestEngineParams: EngineParams,
  val engineParamsScores: Seq[(EngineParams, R)]) {
  
  override def toString: String = {
    implicit lazy val formats = Utils.json4sDefaultFormats +
      new NameParamsSerializer
    
    val bestEPStr = writePretty(bestEngineParams)

    s"""MetricEvaluatorResult:
       |  # engine params evaluated: ${engineParamsScores.size}
       |  best score: ${bestScore}
       |  best engine param: ${bestEPStr}
       |""".stripMargin
  }
}

class MetricEvaluator[EI, Q, P, A, R](
  val metric: Metric[EI, Q, P, A, R])
  extends BaseEvaluator[EI, Q, P, A, MetricEvaluatorResult[R]] {
  @transient lazy val logger = Logger[this.type]
  @transient val engineInstances = Storage.getMetaDataEngineInstances

  def evaluateBase(
    sc: SparkContext,
    engineEvalDataSet: Seq[(EngineParams, Seq[(EI, RDD[(Q, P, A)])])],
    params: WorkflowParams): MetricEvaluatorResult[R] = {

    val evalResultList: Seq[(EngineParams, R)] = engineEvalDataSet
    .zipWithIndex
    .map { case ((engineParams, evalDataSet), idx) => 
      val metricResult: R = metric.calculate(sc, evalDataSet)
      (engineParams, metricResult)
    }

    implicit lazy val formats = Utils.json4sDefaultFormats +
      new NameParamsSerializer

    evalResultList.zipWithIndex.foreach { case ((ep, r), idx) => {
      logger.info(s"Iteration $idx")
      logger.info(s"EngineParams: ${write(ep)}")
      logger.info(s"Result: $r")
    }}

    // use max. take implicit from Metric.
    val (bestEngineParams, bestScore) = evalResultList.reduce(
      (x, y) => (if (metric.compare(x._2, y._2) >= 0) x else y))

    MetricEvaluatorResult(
      bestScore = bestScore,
      bestEngineParams = bestEngineParams,
      engineParamsScores = evalResultList)
  }
}
