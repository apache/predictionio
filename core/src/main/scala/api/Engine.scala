package io.prediction.api

import io.prediction.core.BaseDataSource
import io.prediction.core.BasePreparator
import io.prediction.core.BaseAlgorithm2
import io.prediction.core.BaseServing

class Engine[TD, DP, PD, Q, P, A](
    val dataSourceClass: Class[_ <: BaseDataSource[_ <: Params, DP, TD, Q, A]],
    val preparatorClass: Class[_ <: BasePreparator[_ <: Params, TD, PD]],
    val algorithmClassMap: 
      Map[String, Class[_ <: BaseAlgorithm2[_ <: Params, PD, _, Q, P]]],
    val servingClass: Class[_ <: BaseServing[_ <: Params, Q, P]]
  ) extends Serializable

class EngineParams(
    val dataSourceParams: Params = EmptyParams(),
    val preparatorParams: Params = EmptyParams(),
    val algorithmParamsList: Seq[(String, Params)] = Seq(),
    val servingParams: Params = EmptyParams()) extends Serializable

trait IEngineFactory {
  def apply(): Engine[_, _, _, _, _, _]
}
