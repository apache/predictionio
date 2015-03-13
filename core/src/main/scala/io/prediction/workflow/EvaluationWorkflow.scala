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
import io.prediction.controller.WorkflowParams
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseDataSource
import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEvaluatorResult
import io.prediction.core.BasePreparator
import io.prediction.core.BaseServing
import io.prediction.core.BaseEngine
import io.prediction.core.Doer
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

object EvaluationWorkflow {
  @transient lazy val logger = Logger[this.type]
  def runEvaluation[EI, Q, P, A, R <: BaseEvaluatorResult](
      sc: SparkContext,
      engine: BaseEngine[EI, Q, P, A],
      engineParamsList: Seq[EngineParams],
      evaluator: BaseEvaluator[EI, Q, P, A, R],
      params: WorkflowParams)
    : R = {
    val engineEvalDataSet = engine.batchEval(sc, engineParamsList, params)
    evaluator.evaluateBase(sc, engineEvalDataSet, params)
  }
}
