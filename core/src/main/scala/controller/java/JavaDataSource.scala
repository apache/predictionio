package io.prediction.controller.java

import io.prediction.core.BaseDataSource
import io.prediction.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }

import scala.collection.JavaConversions._
import scala.reflect._

/**
 * Base class of a local data source.
 *
 * A local data source runs locally within a single machine and return data that
 * can fit within a single machine.
 *
 * @param <DSP> Data Source Parameters
 * @param <DP> Data Parameters
 * @param <TD> Training Data
 * @param <Q> Input Query
 * @param <A> Actual Value
 */
abstract class LJavaDataSource[DSP <: Params, DP, TD, Q, A]
  extends BaseDataSource[DSP, DP, RDD[TD], Q, A]()(
    JavaUtils.fakeClassTag[DSP]) {
  def readBase(sc: SparkContext): Seq[(DP, RDD[TD], RDD[(Q, A)])] = {
    implicit val fakeTdTag: ClassTag[TD] = JavaUtils.fakeClassTag[TD]
    read().toSeq.map(e =>
      (e._1, sc.parallelize(Seq(e._2)), sc.parallelize(e._3.toSeq)))
  }

  /** Implement this method to return data from a data source. */
  def read(): JIterable[Tuple3[DP, TD, JIterable[Tuple2[Q, A]]]]
}
