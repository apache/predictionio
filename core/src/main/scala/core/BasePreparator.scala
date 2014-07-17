package io.prediction.core

import io.prediction.api.Params
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

// Probably will add an extra parameter for adhoc json formatter.
abstract class BasePreparator[PP <: Params : ClassTag, TD, PD]
  extends AbstractDoer[PP] {
  def prepareBase(sc: SparkContext, td: TD): PD
}
