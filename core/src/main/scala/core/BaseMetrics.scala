package io.prediction.core

import io.prediction.BaseParams
import scala.reflect._

abstract class BaseMetrics[MP <: BaseParams : ClassTag, 
    DP, Q, P, A, MU, MR, MMR <: AnyRef]
  extends AbstractDoer[MP] {

  def computeUnitBase(input: (Q, P, A)): MU
  
  def computeSetBase(dataParams: DP, metricUnits: Seq[MU]): MR

  def computeMultipleSetsBase(input: Seq[(DP, MR)]): MMR

  /*
  def computeUnit(query: Q, prediction: P, actual: A): MU
  def computeSet(dataParams: DP, metricUnits: Seq[MU]): MR
  def computeMultipleSets(input: Seq[(DP, MR)]): MMR
  */
}

