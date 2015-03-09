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

package io.prediction.controller.experimental

import io.prediction.core.BaseDataSource
import io.prediction.core.BasePreparator
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServing
import io.prediction.core.Doer
import io.prediction.core.BaseEngine
import io.prediction.workflow.CreateWorkflow
import io.prediction.workflow.WorkflowUtils
import io.prediction.workflow.EngineLanguage
import io.prediction.workflow.PersistentModelManifest
import io.prediction.workflow.SparkWorkflowUtils
import io.prediction.workflow.StopAfterReadInterruption
import io.prediction.workflow.StopAfterPrepareInterruption
import io.prediction.data.storage.EngineInstance
import io.prediction.controller.Params
import io.prediction.controller.EngineParams
import io.prediction.controller.Engine
import _root_.java.util.NoSuchElementException
import io.prediction.data.storage.StorageClientException

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.language.implicitConversions

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.read

import io.prediction.workflow.NameParamsSerializer
import grizzled.slf4j.Logger

import scala.collection.mutable.{ HashMap => MutableHashMap }

//sealed abstract 

object FastEvalEngineWorkflow {
  type EX = Int
  type AX = Int
  type QX = Long
  
  @transient lazy val logger = Logger[this.type]

  sealed abstract class BasePrefix {
  }

  case class DataSourcePrefix(
    val dataSourceParams: (String, Params)) extends BasePrefix {
    
    def get[TD, EI, PD, Q, P, A](
      engine: Engine[TD, EI, PD, Q, P, A], 
      cache: Cache[TD, EI, PD, Q, P, A])
    : DataSourceResult[TD, EI, Q, A] = {
      if (!cache.ds.contains(this)) {
        val dataSource = Doer(
          engine.dataSourceClassMap(dataSourceParams._1), 
          dataSourceParams._2)

        val result = dataSource.realEval
        //val result = null.asInstanceOf[DataSourceResult[TD, EI, Q, A]]
        cache.ds += Tuple2(this, result)
      }
      cache.ds(this)
    }
  }

  case class PreparatorPrefix(
    val dataSourceParams: (String, Params),
    val preparatorParams: (String, Params)) extends BasePrefix {
    
    def get[TD, EI, PD, Q, P, A](
      engine: Engine[TD, EI, PD, Q, P, A], 
      cache: Cache[TD, EI, PD, Q, P, A])
    : PreparatorResult[PD] = {
      if (!cache.p.contains(this)) {
        val result = DataSourcePrefix(dataSourceParams).get(engine, cache)
        val r = null.asInstanceOf[PreparatorResult[PD]]
        cache.p += Tuple2(this, r)
      }
      cache.p(this)
    }
  }

  case class AlgorithmsPrefix(
    val dataSourceParams: (String, Params),
    val preparatorParams: (String, Params),
    val algorithmParamsList: Seq[(String, Params)]) extends BasePrefix {

    def get[TD, EI, PD, Q, P, A](
      engine: Engine[TD, EI, PD, Q, P, A], 
      cache: Cache[TD, EI, PD, Q, P, A])
    : AlgorithmsResult[P] = {
      if (!cache.a.contains(this)) {
        val result = PreparatorPrefix(dataSourceParams, preparatorParams)
          .get(engine, cache)
        val r = null.asInstanceOf[AlgorithmsResult[P]]
        cache.a += Tuple2(this, r)
      }
      cache.a(this)
    }
  }

  case class ServingPrefix(
    val dataSourceParams: (String, Params),
    val preparatorParams: (String, Params),
    val algorithmParamsList: Seq[(String, Params)],
    val servingParams: (String, Params)) extends BasePrefix {

    def prefix = AlgorithmsPrefix(
      dataSourceParams,
      preparatorParams,
      algorithmParamsList)
    
    def get[TD, EI, PD, Q, P, A](
      engine: Engine[TD, EI, PD, Q, P, A], 
      cache: Cache[TD, EI, PD, Q, P, A])
    : ServingResult[EI, Q, P, A] = {
      if (!cache.s.contains(this)) {
        val result = prefix.get(engine, cache)
        val r = null.asInstanceOf[ServingResult[EI, Q, P, A]]
        cache.s += Tuple2(this, r)
      }
      cache.s(this)
    }

    def this(ep: EngineParams) = this(
      ep.dataSourceParams,
      ep.preparatorParams,
      ep.algorithmParamsList,
      ep.servingParams)
  }

  sealed abstract class BaseResult {}
  case class EmptyResult() extends BaseResult

  case class DataSourceResult[TD, EI, Q, A](
    val r: Map[EX, (TD, EI, RDD[(Q, A)])]) extends BaseResult

  case class PreparatorResult[PD](val r: Map[EX, PD]) extends BaseResult
  case class AlgorithmsResult[P](val r: Map[EX, RDD[(QX, Seq[P])]]) 
    extends BaseResult
  case class ServingResult[EI, Q, P, A](val r: Seq[(EI, RDD[(Q, P, A)])])
    extends BaseResult

  class Cache[TD, EI, PD, Q, P, A](val engine: Engine[TD, EI, PD, Q, P, A]) {
    val ds = MutableHashMap[DataSourcePrefix, DataSourceResult[TD, EI, Q, A]]()
    val p = MutableHashMap[PreparatorPrefix, PreparatorResult[PD]]()
    val a = MutableHashMap[AlgorithmsPrefix, AlgorithmsResult[P]]()
    val s = MutableHashMap[ServingPrefix, ServingResult[EI, Q, P, A]]()
  }

  def start[TD, EI, PD, Q, P, A](
    engine: Engine[TD, EI, PD, Q, P, A],
    engineParamsList: Seq[EngineParams]) {

    val cache = new Cache(engine)
    //val cache = new MutableHashMap[BasePrefix, BaseResult]()
    engineParamsList.foreach { engineParams => {
      //getResult(engine, new ServingPrefix(engineParams), data)
      (new ServingPrefix(engineParams)).get(engine, cache)
    }}
  }
}

/*
class FastEvalEngineData {
  import io.prediction.controller.experimental.FastEvalEngineWorkflow._
}
*/




/** FastEvalEngine is a subclass of Engine that exploits the immutability of
  * controllers to optimize the evaluation process.
  */
class FastEvalEngine[TD, EI, PD, Q, P, A]
    val dataSourceClassMap: Map[String, Class[_ <: BaseDataSource[TD, EI, Q, A]]],
    val preparatorClassMap: Map[String, Class[_ <: BasePreparator[TD, PD]]],
    val algorithmClassMap: Map[String, Class[_ <: BaseAlgorithm[PD, _, Q, P]]],
    val servingClassMap: Map[String, Class[_ <: BaseServing[Q, P]]])
  extends Engine[TD, EI, PD, Q, P, A](
    dataSourceClassMap,
    preparatorClassMap,
    algorithmClassMap,
    servingClassMap) {

  type EX = Int
  type AX = Int
  type QX = Long
  
  @transient lazy val logger = Logger[this.type]


  override def batchEval(
    sc: SparkContext,
    engineParamsList: Seq[EngineParams],
    params: WorkflowParams)
  : Seq[(EngineParams, Seq[(EI, RDD[(Q, P, A)])])] = {

    Seq[(EngineParams, Seq[(EI, RDD[(Q, P, A)])])]()
  }

}
