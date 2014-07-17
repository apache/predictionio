package io.prediction.controller

import io.prediction.core.BaseAlgorithm
import io.prediction.core.LModelAlgorithm
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class LAlgorithm[
    AP <: Params: ClassTag, PD, M : ClassTag, Q, P]
  extends BaseAlgorithm[AP, RDD[PD], RDD[M], Q, P] 
  with LModelAlgorithm[M, Q, P] {

  def trainBase(sc: SparkContext, pd: RDD[PD]): RDD[M] = pd.map(train)

  def train(pd: PD): M

  def predict(model: M, query: Q): P
}

abstract class P2LAlgorithm[
    AP <: Params: ClassTag, PD, M : ClassTag, Q, P]
  extends BaseAlgorithm[AP, PD, RDD[M], Q, P] 
  with LModelAlgorithm[M, Q, P] {
  // In train: PD => M, M is a local object. We have to parallelize it.
  def trainBase(sc: SparkContext, pd: PD): RDD[M] = {
    val m: M = train(pd)
    sc.parallelize(Array(m))
  }

  def train(pd: PD): M

  def predict(model: M, query: Q): P
}

abstract class PAlgorithm[AP <: Params: ClassTag, PD, M, Q : ClassTag, P]
  extends BaseAlgorithm[AP, PD, M, Q, P] {
  def trainBase(sc: SparkContext, pd: PD): M = train(pd)

  def train(pd: PD): M

  def batchPredictBase(baseModel: Any, indexedQueries: RDD[(Long, Q)])
  : RDD[(Long, P)] = {
    batchPredict(baseModel.asInstanceOf[M], indexedQueries)
  }

  // Evaluation call this method. Since in PAlgorithm, M may contain RDDs, it
  // is impossible to call the "predict" method without localizing queries
  // (which is very inefficient). Hence, engine builders using PAlgorithms need
  // to implement "batchPredict" for evaluation purpose.
  def batchPredict(model: M, indexedQueries: RDD[(Long, Q)]): RDD[(Long, P)]

  def predictBase(baseModel: Any, query: Q): P = {
    predict(baseModel.asInstanceOf[M], query)
  }

  // Deployment call this method
  def predict(model: M, query: Q): P
}

