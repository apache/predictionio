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
/*
import io.prediction.controller.java.LJavaDataSource
import io.prediction.controller.java.LJavaPreparator
import io.prediction.controller.java.LJavaAlgorithm
import io.prediction.controller.java.LJavaServing
import io.prediction.controller.java.JavaEvaluator
import io.prediction.controller.java.JavaUtils
import io.prediction.controller.java.JavaEngine
import io.prediction.controller.java.PJavaAlgorithm
*/
import io.prediction.controller.WorkflowParams
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseDataSource
import io.prediction.core.BaseEvaluator
import io.prediction.core.BasePreparator
import io.prediction.core.BaseServing
import io.prediction.core.BaseEngine
import io.prediction.core.Doer
// import io.prediction.core.LModelAlgorithm
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
import org.json4s.native.Serialization.writePretty


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

/*
// FIXME: move to better location.
object WorkflowContext extends Logging {
  def apply(
      batch: String = "",
      executorEnv: Map[String, String] = Map(),
      sparkEnv: Map[String, String] = Map(),
      mode: String = ""
    ): SparkContext = {
    val conf = new SparkConf()
    val prefix = if (mode == "") "PredictionIO" else s"PredictionIO ${mode}"
    conf.setAppName(s"${prefix}: ${batch}")
    debug(s"Executor environment received: ${executorEnv}")
    executorEnv.map(kv => conf.setExecutorEnv(kv._1, kv._2))
    debug(s"SparkConf executor environment: ${conf.getExecutorEnv}")
    debug(s"Application environment received: ${sparkEnv}")
    conf.setAll(sparkEnv)
    val sparkConfString = conf.getAll.toSeq
    debug(s"SparkConf environment: $sparkConfString")
    new SparkContext(conf)
  }
}
*/

/*
Ideally, Java could also act as other scala base class. But the tricky part
is in the algorithmClassMap, where javac is not smart enough to match
JMap[String, Class[_ <: LJavaAlgorith[...]]] (which is provided by the
caller) with JMap[String, Class[_ <: BaseAlgo[...]]] (signature of this
function). If we change the caller to use Class[_ <: BaseAlgo[...]], it is
difficult for the engine builder, as we wrap data structures with RDD in the
base class. Hence, we have to sacrifices here, that all Doers calling
JavaCoreWorkflow needs to be Java sub-doers.
*/

/*
object JavaCoreWorkflow {
  def noneIfNull[T](t: T): Option[T] = (if (t == null) None else Some(t))

  // Java doesn't support default parameters. If you only want to test, say,
  // DataSource and PreparatorClass only, please pass null to the other
  // components.
  // Another method is to use JavaEngineBuilder, add only the components you
  // already have. It will handle the missing ones.
  def run[
      EI, TD, PD, Q, P, A, ER <: AnyRef](
    env: JMap[String, String] = new JHashMap(),
    dataSourceClassMap: JMap[String, Class[_ <: BaseDataSource[TD, EI, Q, A]]],
    dataSourceParams: (String, Params),
    preparatorClassMap: JMap[String, Class[_ <: BasePreparator[TD, PD]]],
    preparatorParams: (String, Params),
    algorithmClassMap:
      JMap[String, Class[_ <: BaseAlgorithm[PD, _, Q, P]]],
    algorithmParamsList: JIterable[(String, Params)],
    servingClassMap: JMap[String, Class[_ <: BaseServing[Q, P]]],
    servingParams: (String, Params),
    evaluatorClass: Class[_ <: BaseEvaluator[EI, Q, P, A, ER]],
    evaluatorParams: Params,
    params: WorkflowParams
  ) = {
    throw new NotImplementedError("JavaCoreWorkflow is deprecated as of 0.8.7.")
  }
}
*/
