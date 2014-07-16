package io.prediction.api.java

import io.prediction.core.BaseDataSource
import io.prediction.api.Params

import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._

import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.reflect._

abstract class LJavaDataSource[DSP <: Params, DP, TD, Q, A]
  extends BaseDataSource[DSP, DP, RDD[TD], Q, A]()(
    JavaUtils.fakeClassTag[DSP]) {
  def readBase(sc: SparkContext): Seq[(DP, RDD[TD], RDD[(Q, A)])] = {
    implicit val fakeTdTag: ClassTag[TD] = JavaUtils.fakeClassTag[TD]
    read().toSeq.map(e => 
      (e._1, sc.parallelize(Seq(e._2)), sc.parallelize(e._3.toSeq)))
  }

  def read(): JIterable[Tuple3[DP, TD, JIterable[Tuple2[Q, A]]]]
}
