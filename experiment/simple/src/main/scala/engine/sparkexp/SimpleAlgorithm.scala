package io.prediction.engines.sparkexp

import io.prediction.SparkAlgorithm

import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class SimpleAlgorithm(val sc: SparkContext)
  extends SparkAlgorithm[TD, F, P, M, AP] {

  override def init(algoParams: AP): Unit = {}

  override def train(trainingData: TD): M = {
    // dummy RDD processing
    val mod1: RDD[(String, String)] = trainingData.d1
      .map{ case (id, x) => (id, s"${x}m1") }
    val mod2: RDD[(String, String)] = trainingData.d2
      .map{ case (id, x) => (id, s"${x}m2") }
    new M(
     m1 = mod1,
     m2 = mod2
    )
  }

  override def predict(model: M, feature: F): P = {
    val d1 = model.m1.lookup(feature.f).head
    val d2 = model.m2.lookup(feature.f).head
    val f = feature.f
    val res = s"${d1}+${d2}+${f}"
    new P(p = res)
  }

  override def predictBatch(model: M, feature: RDD[(Long, F)]): RDD[(Long, P)] = {
    feature.map{case(idx, f) => (f.f, idx)}
      .join(model.m1) // (f.f, (idx, m1.d))
      .join(model.m2) // (f.f, ((idx, m1.d), m2.d))
      .map { case (f, ((idx, d1), d2)) =>
        val res = s"${d1}+${d2}+${f}"
        (idx, new P(p = res))
      }
  }
}
