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

import io.prediction.core.BaseAlgorithm
import io.prediction.workflow.PersistentModelManifest
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect._

/** Base class of a parallel algorithm.
  *
  * A parallel algorithm can be run in parallel on a cluster and produces a
  * model that can also be distributed across a cluster.
  *
  * If your input query class requires custom JSON4S serialization, the most
  * idiomatic way is to implement a trait that extends [[CustomQuerySerializer]],
  * and mix that into your algorithm class, instead of overriding
  * [[querySerializer]] directly.
  *
  * To provide evaluation feature, one must override and implement the
  * [[batchPredict]] method. Otherwise, an exception will be thrown when pio eval`
  * is used.
  *
  * @tparam PD Prepared data class.
  * @tparam M Trained model class.
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  * @group Algorithm
  */
abstract class PAlgorithm[PD, M, Q : Manifest, P]
  extends BaseAlgorithm[PD, M, Q, P] {

  /** Do not use directly or override this method, as this is called by
    * PredictionIO workflow to train a model.
    */
  private[prediction]
  def trainBase(sc: SparkContext, pd: PD): M = train(sc, pd)

  /** Implement this method to produce a model from prepared data.
    *
    * @param pd Prepared data for model training.
    * @return Trained model.
    */
  def train(sc: SparkContext, pd: PD): M

  private[prediction]
  def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Q)])
  : RDD[(Long, P)] = batchPredict(bm.asInstanceOf[M], qs)


  /** To provide evaluation feature, one must override and implement this method
    * to generate many predictions in batch. Otherwise, an exception will be
    * thrown when `pio eval` is used.
    *
    * The default implementation throws an exception.
    *
    * @param m Trained model produced by [[train]].
    * @param qs An RDD of index-query tuples. The index is used to keep track of
    *           predicted results with corresponding queries.
    */
  def batchPredict(m: M, qs: RDD[(Long, Q)]): RDD[(Long, P)] =
    throw new NotImplementedError("batchPredict not implemented")

  /** Do not use directly or override this method, as this is called by
    * PredictionIO workflow to perform prediction.
    */
  private[prediction]
  def predictBase(baseModel: Any, query: Q): P = {
    predict(baseModel.asInstanceOf[M], query)
  }

  /** Implement this method to produce a prediction from a query and trained
    * model.
    *
    * @param model Trained model produced by [[train]].
    * @param query An input query.
    * @return A prediction.
    */
  def predict(model: M, query: Q): P

  private[prediction]
  override
  def makePersistentModel(
    sc: SparkContext,
    modelId: String,
    algoParams: Params,
    bm: Any): Any = {
    // In general, a parallel model may contain multiple RDDs. It is not easy to
    // infer and persist them programmatically since these RDDs may be
    // potentially huge.

    // Persist is successful only if the model is an instance of
    // IPersistentModel and save is successful.
    val m = bm.asInstanceOf[M]
    if (m.isInstanceOf[PersistentModel[_]]) {
      if (m.asInstanceOf[PersistentModel[Params]].save(
        modelId, algoParams, sc)) {
        PersistentModelManifest(className = m.getClass.getName)
      } else {
        Unit
      }
    } else {
      Unit
    }
  }
}
