package io.prediction.core

import io.prediction.BaseParams
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

// FIXME. The name collides with current BaseAlgorithm. Will remove once the
// code is completely revamped.

abstract class BaseAlgorithm2[AP <: BaseParams : ClassTag, PD, M, Q, P](ap: AP)
  extends AbstractDoer[AP](ap) {
  def trainBase(sc: SparkContext, pd: PD): M

  def batchPredictBase(baseModel: Any, baseQueries: RDD[(Long, Q)])
  : RDD[(Long, P)]
}

