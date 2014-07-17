package io.prediction.api.java

import io.prediction.api.Engine
import io.prediction.api.Params
import io.prediction.api.EngineParams

import scala.collection.JavaConversions._
import java.lang.{ Iterable => JIterable }
import java.util.{ Map => JMap }

class JavaEngine[TD, DP, PD, Q, P, A](
    dataSourceClass: Class[_ <: LJavaDataSource[_ <: Params, DP, TD, Q, A]],
    preparatorClass: Class[_ <: LJavaPreparator[_ <: Params, TD, PD]],
    algorithmClassMap
      : JMap[String, Class[_ <: LJavaAlgorithm[_ <: Params, PD, _, Q, P]]],
    servingClass: Class[_ <: LJavaServing[_ <: Params, Q, P]]
) extends Engine(
    dataSourceClass, 
    preparatorClass,
    Map(algorithmClassMap.toSeq: _*),
    servingClass)

class JavaEngineParams(
    dataSourceParams: Params,
    preparatorParams: Params,
    algorithmParamsList: JIterable[(String, Params)],
    servingParams: Params
) extends EngineParams(
    dataSourceParams,
    preparatorParams,
    algorithmParamsList.toSeq,
    servingParams)
    

