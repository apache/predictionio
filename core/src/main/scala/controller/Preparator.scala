package io.prediction.controller

import io.prediction.core.BaseDataSource
import io.prediction.core.BasePreparator

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.json4s.Formats
import org.json4s.native.Serialization

import scala.reflect._
import scala.reflect.runtime.universe._

abstract class LPreparator[
    PP <: Params : ClassTag, TD, PD: ClassTag]
  extends BasePreparator[PP, RDD[TD], RDD[PD]] {

  def prepareBase(sc: SparkContext, rddTd: RDD[TD]): RDD[PD] = {
    rddTd.map(prepare)
  }

  def prepare(trainingData: TD): PD

  @transient lazy val formats: Formats = Utils.json4sDefaultFormats

  def stringToTD[TD : TypeTag : ClassTag](trainingData: String): TD = {
    implicit val f = formats
    Serialization.read[TD](trainingData)
  }
}

abstract class PPreparator[PP <: Params : ClassTag, TD, PD]
  extends BasePreparator[PP, TD, PD] {

  def prepareBase(sc: SparkContext, td: TD): PD = {
    prepare(sc, td)
    // TODO: Optinally check pd size. Shouldn't exceed a few KB.
  }

  def prepare(sc: SparkContext, trainingData: TD): PD
}

/******* Helper Functions ******/
class IdentityPreparator[TD] extends BasePreparator[EmptyParams, TD, TD] {
  def prepareBase(sc: SparkContext, td: TD): TD = td
}

object IdentityPreparator {
  def apply[TD](ds: Class[_ <: BaseDataSource[_, _, TD, _, _]]) =
    classOf[IdentityPreparator[TD]]
}
