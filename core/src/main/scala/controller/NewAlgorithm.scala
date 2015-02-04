package n.io.prediction.controller

import n.io.prediction.core.BaseAlgorithm
import io.prediction.core.AbstractDoer
//import new.io.prediction.core.LModelAlgorithm
//import io.prediction.core.WithBaseQuerySerializer

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
  def trainBase(sc: SparkContext, pd: PD): M = train(pd)

  /** Implement this method to produce a model from prepared data.
    *
    * @param pd Prepared data for model training.
    * @return Trained model.
    */
  def train(pd: PD): M
  
  def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Q)]) 
  : RDD[(Long, P)] = batchPredict(bm.asInstanceOf[M], qs)

  def batchPredict(m: M, qs: RDD[(Long, Q)]): RDD[(Long, P)]

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
  def trainBase(sc: SparkContext, pd: PD): M = train(pd)

  /** Implement this method to produce a model from prepared data.
    *
    * @param pd Prepared data for model training.
    * @return Trained model.
    */
  def train(pd: PD): M
  
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
}

