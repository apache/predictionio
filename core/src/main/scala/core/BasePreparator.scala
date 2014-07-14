package io.prediction.core

import io.prediction.BaseParams
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

// Probably will add an extra parameter for adhoc json formatter.
abstract class BasePreparator[PP <: BaseParams : ClassTag, TD, PD](pp: PP)
  extends AbstractDoer[PP](pp) {
  def prepareBase(sc: SparkContext, td: TD): PD

  // Not sure if we should wrap it with RDD for LocalPreparator...
  //def jsonAsTrainingData(json JValue): TD
}


