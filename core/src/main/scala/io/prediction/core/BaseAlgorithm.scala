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

import io.prediction.controller.Params
import io.prediction.controller.Utils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect._

trait BaseQuerySerializer {
  @transient lazy val querySerializer = Utils.json4sDefaultFormats
}

abstract class BaseAlgorithm[PD, M, Q : Manifest, P]
  extends AbstractDoer with BaseQuerySerializer {
  private[prediction]
  def trainBase(sc: SparkContext, pd: PD): M

  // Used by Evaluation
  private[prediction]
  def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Q)])
  : RDD[(Long, P)]

  // Used by Deploy
  private[prediction]
  def predictBase(bm: Any, q: Q): P

  private[prediction]
  def queryManifest(): Manifest[Q] = manifest[Q]

  private[prediction]
  def makePersistentModel(
    sc: SparkContext,
    modelId: String,
    algoParams: Params,
    bm: Any): Any = Unit
}
