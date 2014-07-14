package io.prediction.core

import io.prediction.BaseParams
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class BasePreparator[PP <: BaseParams : ClassTag, TD, PD](pp: PP)
  extends AbstractDoer[PP](pp) {
  def prepareBase(sc: SparkContext, td: TD): PD

  // TODO(yipjustin). Convert json to TD
}


