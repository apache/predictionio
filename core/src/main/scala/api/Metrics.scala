package io.prediction.api

import io.prediction.core.BaseMetrics
import io.prediction.EmptyParams
import io.prediction.BaseParams
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class Metrics[MP <: BaseParams : ClassTag,
    DP, Q, P, A, MU, MR, MMR <: AnyRef]
  extends BaseMetrics[MP, DP, Q, P, A, MU, MR, MMR] {
  def computeUnitBase(input: (Q, P, A)): MU = {
    computeUnit(input._1, input._2, input._3) 
  }

  def computeUnit(query: Q, prediction: P, actual: A): MU

  def computeSetBase(dataParams: DP, metricUnits: Seq[MU]): MR = {
    computeSet(dataParams, metricUnits)
  }

  def computeSet(dataParams: DP, metricUnits: Seq[MU]): MR

  def computeMultipleSetsBase(input: Seq[(DP, MR)]): MMR = {
    computeMultipleSets(input)
  }
  
  def computeMultipleSets(input: Seq[(DP, MR)]): MMR
}



