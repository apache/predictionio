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
// import io.prediction.core.LModelAlgorithm
import io.prediction.core.WithBaseQuerySerializer
import io.prediction.workflow.PersistentModelManifest

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._

/** Base class of a parallel algorithm.
  *
  * A parallel algorithm can be run in parallel on a cluster and produces a
  * model that can also be distributed across a cluster.
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
  def trainBase(sc: SparkContext, pd: PD): M = train(sc, pd)

  /** Implement this method to produce a model from prepared data.
    *
    * @param pd Prepared data for model training.
    * @return Trained model.
    */
  def train(sc: SparkContext, pd: PD): M
  
  def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Q)]) 
  : RDD[(Long, P)] = batchPredict(bm.asInstanceOf[M], qs)

  def batchPredict(m: M, qs: RDD[(Long, Q)]): RDD[(Long, P)] = {
    throw new NotImplementedError("batchPredict not implemented")
    qs.context.emptyRDD[(Long, P)]
  }

  /** Do not use directly or override this method, as this is called by
    * PredictionIO workflow to perform prediction.
    */
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
 
  override
  def makePersistentModel(sc: SparkContext, modelId: String, 
    algoParams: Params, bm: Any)
  : Any = {
    // In general, a parallel model may contain multiple RDDs. It is not easy to
    // infer and persist them programmatically since these RDDs may be
    // potentially huge.

    // Persist is successful only if the model is an instance of
    // IPersistentModel and save is successful.
    val m = bm.asInstanceOf[M]
    if (m.isInstanceOf[IPersistentModel[_]]) {
      if (m.asInstanceOf[IPersistentModel[Params]].save(
        modelId, algoParams, sc)) {
        PersistentModelManifest(className = m.getClass.getName)
      } else {
        Unit
      }
    } else {
      Unit
    }
  }
  
  def isJava: Boolean = false
  // def isParallel = true
}

/** Base class of a local algorithm.
  *
  * A local algorithm runs locally within a single machine and produces a model
  * that can fit within a single machine.
  *
  * @tparam PD Prepared data class.
  * @tparam M Trained model class.
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  * @group Algorithm
  */
abstract class LAlgorithm[PD, M : ClassTag, Q : Manifest, P]
  extends BaseAlgorithm[RDD[PD], RDD[M], Q, P] {

  /** Do not use directly or override this method, as this is called by
    * PredictionIO workflow to train a model.
    */
  def trainBase(sc: SparkContext, pd: RDD[PD]): RDD[M] = pd.map(train)

  /** Implement this method to produce a model from prepared data.
    *
    * @param pd Prepared data for model training.
    * @return Trained model.
    */
  def train(pd: PD): M

  def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Q)])
  : RDD[(Long, P)] = {
    val mRDD = bm.asInstanceOf[RDD[M]]
    batchPredict(mRDD, qs)
  }

  def batchPredict(mRDD: RDD[M], qs: RDD[(Long, Q)]): RDD[(Long, P)] = {
    val glomQs: RDD[Array[(Long, Q)]] = qs.glom()
    val cartesian: RDD[(M, Array[(Long, Q)])] = mRDD.cartesian(glomQs)
    cartesian.flatMap { case (m, qArray) => {
      qArray.map { case (qx, q) => (qx, predict(m, q)) }
    }}
  }

  def predictBase(localBaseModel: Any, q: Q): P = {
    predict(localBaseModel.asInstanceOf[M], q)
  }

  /** Implement this method to produce a prediction from a query and trained
    * model.
    *
    * @param model Trained model produced by [[train]].
    * @param query An input query.
    * @return A prediction.
    */
  def predict(m: M, q: Q): P
  
  override
  def makePersistentModel(sc: SparkContext, modelId: String, 
    algoParams: Params, bm: Any)
  : Any = {
    // LAlgo has local model. By default, the model is serialized into our
    // storage automatically. User can override this by implementing the
    // IPersistentModel trait, then we call the save method, upon successful, we
    // return the Manifest, otherwise, Unit. 

    // Check RDD[M].count == 1
    val m = bm.asInstanceOf[RDD[M]].first
    if (m.isInstanceOf[IPersistentModel[_]]) {
      if (m.asInstanceOf[IPersistentModel[Params]].save(
        modelId, algoParams, sc)) {
        PersistentModelManifest(className = m.getClass.getName)
      } else {
        Unit
      }
    } else {
      m
    }
  }
  
  def isJava: Boolean = false
  // def isParallel = true
}

/** Base class of a parallel-to-local algorithm.
  *
  * A parallel-to-local algorithm can be run in parallel on a cluster and
  * produces a model that can fit within a single machine.
  *
  * @tparam PD Prepared data class.
  * @tparam M Trained model class.
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  * @group Algorithm
  */
abstract class P2LAlgorithm[PD, M : ClassTag, Q : Manifest, P]
  extends BaseAlgorithm[PD, M, Q, P] {

  /** Do not use directly or override this method, as this is called by
    * PredictionIO workflow to train a model.
    */
  def trainBase(sc: SparkContext, pd: PD): M = train(sc, pd)

  /** Implement this method to produce a model from prepared data.
    *
    * @param pd Prepared data for model training.
    * @return Trained model.
    */
  def train(sc: SparkContext, pd: PD): M
  
  def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Q)]) 
  : RDD[(Long, P)] = batchPredict(bm.asInstanceOf[M], qs)

  def batchPredict(m: M, qs: RDD[(Long, Q)]): RDD[(Long, P)] = {
    qs.mapValues { q => predict(m, q) }
  }

  def predictBase(bm: Any, q: Q): P = predict(bm.asInstanceOf[M], q)

  /** Implement this method to produce a prediction from a query and trained
    * model.
    *
    * @param model Trained model produced by [[train]].
    * @param query An input query.
    * @return A prediction.
    */
  def predict(model: M, query: Q): P
  
  override
  def makePersistentModel(sc: SparkContext, modelId: String, 
    algoParams: Params, bm: Any)
  : Any = {
    // P2LAlgo has local model. By default, the model is serialized into our
    // storage automatically. User can override this by implementing the
    // IPersistentModel trait, then we call the save method, upon successful, we
    // return the Manifest, otherwise, Unit. 

    val m = bm.asInstanceOf[M]
    if (m.isInstanceOf[IPersistentModel[_]]) {
      if (m.asInstanceOf[IPersistentModel[Params]].save(
        modelId, algoParams, sc)) {
        PersistentModelManifest(className = m.getClass.getName)
      } else {
        Unit
      }
    } else {
      m
    }
  }
  
  
  def isJava: Boolean = false
  // def isParallel = true
}

/** Implement in this trait to enable custom json4s serializer.
  * This is useful when your query requires a custom serializer. The algorithm
  * classes using this query only need to mix in the trait to enable the custom
  * serializer.
  *
  * @group General
  */
trait WithQuerySerializer extends WithBaseQuerySerializer
