package io.prediction.controller.java

import io.prediction.core.BasePreparator
import io.prediction.controller.Params

import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._

import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.reflect._

abstract class LJavaPreparator[PP <: Params, TD, PD]
  extends BasePreparator[PP, RDD[TD], RDD[PD]]() (
    JavaUtils.fakeClassTag[PP]) {

  def prepareBase(sc: SparkContext, td: RDD[TD]): RDD[PD] = {
    implicit val fakeTdTag: ClassTag[PD] = JavaUtils.fakeClassTag[PD]
    td.map(prepare)
  }

  def prepare(td: TD): PD
}

