package io.prediction.controller.java

import io.prediction.controller.LAlgorithm
import io.prediction.controller.Params
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
