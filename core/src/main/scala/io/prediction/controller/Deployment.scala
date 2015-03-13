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

import io.prediction.core.BaseDataSource
import io.prediction.core.BasePreparator
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServing
import io.prediction.core.Doer
import io.prediction.core.BaseEngine
// import io.prediction.workflow.EngineWorkflow
import io.prediction.workflow.CreateWorkflow
import io.prediction.workflow.WorkflowUtils
import io.prediction.workflow.EngineLanguage
import io.prediction.workflow.PersistentModelManifest
import io.prediction.workflow.SparkWorkflowUtils
import io.prediction.workflow.StopAfterReadInterruption
import io.prediction.workflow.StopAfterPrepareInterruption
import io.prediction.data.storage.EngineInstance
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

/** Defines a deployment that contains an engine.
  *
  * @group Engine
  */
trait Deployment extends IEngineFactory {
  protected[this] var _engine: BaseEngine[_, _, _, _] = _
  protected[this] var engineSet: Boolean = false

  /** Returns the [[Engine]] contained in this [[Deployment]]. */
  def apply(): BaseEngine[_, _, _, _] = {
    assert(engineSet, "Engine not set")
    _engine
  }

  /** Returns the [[Engine]] contained in this [[Deployment]]. */
  private [prediction] 
  def engine: BaseEngine[_, _, _, _] = {
    assert(engineSet, "Engine not set")
    _engine
  }

  /** Sets the [[Engine]] for this [[Deployment]]. */
  def engine_=[EI, Q, P, A](engine: BaseEngine[EI, Q, P, A]) {
    assert(!engineSet, "Engine can be set at most once")
    _engine = engine
    engineSet = true
  }
}


