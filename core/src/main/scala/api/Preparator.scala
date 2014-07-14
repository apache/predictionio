package io.prediction.api

import io.prediction.core.BasePreparator
import io.prediction.EmptyParams
import io.prediction.BaseParams
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class LPreparator[
    PP <: BaseParams : Manifest, TD, PD: ClassTag](pp: PP)
  extends BasePreparator[PP, RDD[TD], RDD[PD]](pp) {

  def prepareBase(sc: SparkContext, rddTd: RDD[TD]): RDD[PD] = {
    rddTd.map(prepare)
  }
  
  def prepare(trainingData: TD): PD
}

