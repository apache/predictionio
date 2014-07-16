package io.prediction.api.java

import io.prediction.core.BaseMetrics
import io.prediction.api.Params

import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._

import scala.reflect._

abstract class JavaMetrics[MP <: Params, DP, Q, P, A, MU, MR, MMR <: AnyRef]
  extends BaseMetrics[MP, DP, Q, P, A, MU, MR, MMR]()(
    JavaUtils.fakeManifest[MP]) {
  
  def computeUnitBase(input: (Q, P, A)): MU = {
    computeUnit(input._1, input._2, input._3)
  }

  def computeUnit(query: Q, predicted: P, actual: A): MU
  
  def computeSetBase(dataParams: DP, metricUnits: Seq[MU]): MR = {
    computeSet(dataParams, metricUnits)
  }

  def computeSet(dataParams: DP, metricUnits: JIterable[MU]): MR

  def computeMultipleSetsBase(input: Seq[(DP, MR)]): MMR = {
    computeMultipleSets(input)
  }

  def computeMultipleSets(input: JIterable[Tuple2[DP, MR]]): MMR
}
