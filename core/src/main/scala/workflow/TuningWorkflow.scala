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

package io.prediction.workflow

import io.prediction.controller.EmptyParams
import io.prediction.controller.Engine
import io.prediction.controller.EngineParams
import io.prediction.controller.IPersistentModel
import io.prediction.controller.LAlgorithm
import io.prediction.controller.PAlgorithm
import io.prediction.controller.Metric
import io.prediction.controller.Params
import io.prediction.controller.Utils
import io.prediction.controller.NiceRendering
import io.prediction.controller.SanityCheck
import io.prediction.controller.java.LJavaDataSource
import io.prediction.controller.java.LJavaPreparator
import io.prediction.controller.java.LJavaAlgorithm
import io.prediction.controller.java.LJavaServing
import io.prediction.controller.java.JavaEvaluator
import io.prediction.controller.java.JavaUtils
import io.prediction.controller.java.JavaEngine
import io.prediction.controller.java.PJavaAlgorithm
import io.prediction.controller.WorkflowParams
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseDataSource
import io.prediction.core.BaseEvaluator
import io.prediction.core.BasePreparator
import io.prediction.core.BaseServing
import io.prediction.core.BaseEngine
import io.prediction.core.Doer
//import io.prediction.core.LModelAlgorithm
import io.prediction.data.storage.EngineInstance
import io.prediction.data.storage.EngineInstances
import io.prediction.data.storage.Model
import io.prediction.data.storage.Storage

import com.github.nscala_time.time.Imports.DateTime
import com.twitter.chill.KryoInjection
import grizzled.slf4j.{ Logger, Logging }
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.json4s._
import org.json4s.native.Serialization.write

import scala.collection.JavaConversions._
import scala.language.existentials
import scala.reflect.ClassTag
import scala.reflect.Manifest

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.lang.{ Iterable => JIterable }
import java.util.{ HashMap => JHashMap, Map => JMap }
import org.json4s._
import org.json4s.native.Serialization.write

object TuningWorkflow {
  @transient lazy val logger = Logger[this.type]
  @transient val engineInstances = Storage.getMetaDataEngineInstances

  class TuningEvaluator[EI, Q, P, A, R](metric: Metric[EI, Q, P, A, R])
  extends BaseEvaluator[EI, Q, P, A, R] {
    def evaluateBase(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])])
    : R = {
      metric.calculate(sc, evalDataSet)
    }
  }

  def runTuning[EI, Q, P, A, R](
      sc: SparkContext,
      engine: BaseEngine[EI, Q, P, A],
      engineParamsList: Seq[EngineParams],
      metric: Metric[EI, Q, P, A, R],
      params: WorkflowParams = WorkflowParams()): (EngineParams, R) = {
    logger.info("TuningWorkflow.runTuning completed.")

    val tuningEvaluator = new TuningEvaluator(metric)

    val evalResultList: Seq[(EngineParams, R)] = engineParamsList
    .zipWithIndex
    .map { case (engineParams, idx) => {
      val evalDataSet: Seq[(EI, RDD[(Q, P, A)])] = engine.eval(sc, engineParams)

      val metricResult: R = tuningEvaluator.evaluateBase(sc, evalDataSet)


      (engineParams, metricResult)
    }}
   
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

    logger.info("TuningWorkflow.runTuning completed.")
    (bestEngineParams, bestScore)
  }
}


