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

package io.prediction.core

import io.prediction.controller.Utils
import io.prediction.controller.EngineParams
import io.prediction.controller.WorkflowParams

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import scala.reflect._
import org.json4s.JValue

abstract class BaseEngine[EI, Q, P, A] extends Serializable {
  // Return Persist-able models
  def train(
    sc: SparkContext, 
    engineParams: EngineParams,
    engineInstanceId: String,
    params: WorkflowParams): Seq[Any]

  def eval(
    sc: SparkContext, 
    engineParams: EngineParams,
    params: WorkflowParams
  )
  : Seq[(EI, RDD[(Q, P, A)])]

  /** Can subclass this method to optimize the case where multiple evaluation is
    * called (during tuning, for example). By default, it call [[eval]] for each
    * element in the engine params list.
    */
  def batchEval(
    sc: SparkContext, 
    engineParamsList: Seq[EngineParams],
    params: WorkflowParams)
  : Seq[(EngineParams, Seq[(EI, RDD[(Q, P, A)])])] = {
    engineParamsList.map { engineParams => 
      (engineParams, eval(sc, engineParams, params))
    }
  }

  def jValueToEngineParams(variantJson: JValue): EngineParams = {
    throw new NotImplementedError("json to EngineParams is not implemented.")
    EngineParams()  
  }
}
