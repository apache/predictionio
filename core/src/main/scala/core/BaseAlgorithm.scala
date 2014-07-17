package io.prediction.core

import io.prediction.api.Params
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

// FIXME. The name collides with current BaseAlgorithm. Will remove once the
// code is completely revamped.

abstract class BaseAlgorithm2[AP <: Params : ClassTag, PD, M, Q, P]
  extends AbstractDoer[AP] {
  def trainBase(sc: SparkContext, pd: PD): M

  def batchPredictBase(baseModel: Any, baseQueries: RDD[(Long, Q)])
  : RDD[(Long, P)]

  // One Prediction
  def predictBase(baseModel: Any, query: Q): P
}

trait LModelAlgorithm[M, Q, P] {
  def getModel(baseModel: Any): RDD[Any] = {
    baseModel.asInstanceOf[RDD[Any]]
  }

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


