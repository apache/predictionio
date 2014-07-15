package io.prediction.api

import io.prediction.core.BaseAlgorithm2
import io.prediction.EmptyParams
import io.prediction.BaseParams
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class LAlgorithm[
    AP <: BaseParams : ClassTag : Manifest, PD, M : ClassTag, Q, P]
  extends BaseAlgorithm2[AP, RDD[PD], RDD[M], Q, P] {
  def getModel(baseModel: Any): RDD[Any] = {
    baseModel.asInstanceOf[RDD[Any]]
  }

  def trainBase(sc: SparkContext, pd: RDD[PD]): RDD[M] = pd.map(train)

  def train(pd: PD): M

  def batchPredictBase(baseModel: Any, baseQueries: RDD[(Long, Q)])
  : RDD[(Long, P)] = {
    val rddModel: RDD[M] = baseModel.asInstanceOf[RDD[M]].coalesce(1)
    val rddQueries: RDD[(Long, Q)] = baseQueries.coalesce(1)

    rddModel.zipPartitions(rddQueries)(batchPredictWrapper)
  }

  def batchPredictWrapper(model: Iterator[M], queries: Iterator[(Long, Q)])
  : Iterator[(Long, P)] = {
    batchPredict(model.next, queries)
  }

  // Expected to be overridden for performance consideration
  def batchPredict(model: M, queries: Iterator[(Long, Q)])
  : Iterator[(Long, P)] = {
    queries.map { case (idx, q) => (idx, predict(model, q)) }
  } 

  // One Prediction
  def predictBase(localBaseModel: Any, query: Q): P = {
    predict(localBaseModel.asInstanceOf[M], query) 
  }

  def predict(model: M, query: Q): P
}

