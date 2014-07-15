package io.prediction.api

import io.prediction.core.BaseAlgorithm2
import io.prediction.core.LModelAlgorithm
//import io.prediction.api.EmptyParams
//import io.prediction.BaseParams
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class LAlgorithm[
    AP <: Params: ClassTag : Manifest, PD, M : ClassTag, Q, P]
  extends BaseAlgorithm2[AP, RDD[PD], RDD[M], Q, P] 
  with LModelAlgorithm[M, Q, P] {

  def trainBase(sc: SparkContext, pd: RDD[PD]): RDD[M] = pd.map(train)

  def train(pd: PD): M

  def predict(model: M, query: Q): P
}

abstract class P2LAlgorithm[
    AP <: Params: ClassTag : Manifest, PD, M : ClassTag, Q, P]
  extends BaseAlgorithm2[AP, PD, RDD[M], Q, P] 
  with LModelAlgorithm[M, Q, P] {
  // In train: PD => M, M is a local object. We have to parallelize it.
  def trainBase(sc: SparkContext, pd: PD): RDD[M] = {
    val m: M = train(pd)
    sc.parallelize(Array(m))
  }

  def train(pd: PD): M

  def predict(model: M, query: Q): P
}

