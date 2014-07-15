package io.prediction.core

import io.prediction.api.Params
import scala.reflect._

abstract class BaseMetrics[MP <: Params : ClassTag, 
    -DP, Q, P, A, MU, MR, MMR <: AnyRef]
  extends AbstractDoer[MP] {

  def computeUnitBase(input: (Q, P, A)): MU
  
  def computeSetBase(dataParams: DP, metricUnits: Seq[MU]): MR

  def computeMultipleSetsBase(input: Seq[(DP, MR)]): MMR
}

