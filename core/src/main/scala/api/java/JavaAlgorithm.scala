package io.prediction.api.java

import io.prediction.api.LAlgorithm
import io.prediction.api.Params
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.reflect._

abstract class LJavaAlgorithm[AP <: Params, PD, M, Q, P]
  extends LAlgorithm[AP, PD, M, Q, P]()(
    JavaUtils.fakeClassTag[AP], JavaUtils.fakeClassTag[M]) {
  def train(pd: PD): M

  def predict(model: M, query: Q): P
}
