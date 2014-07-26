package io.prediction.controller.java

import io.prediction.controller.Engine
import io.prediction.controller.Params
import io.prediction.controller.EngineParams

import java.lang.{ Iterable => JIterable }
import java.util.{ Map => JMap }

import scala.collection.JavaConversions._

/**
 * This class chains up the entire data process. PredictionIO uses this
 * information to create workflows and deployments. In Java, use
 * JavaEngineBuilder to conveniently instantiate an instance of this class.
 *
 * @param <TD> Training Data
 * @param <DP> Data Parameters
 * @param <PD> Prepared Data
 * @param <Q> Input Query
 * @param <P> Output Prediction
 * @param <A> Actual Value
 */
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

/**
 * This class serves as a logical grouping of all required engine's parameters.
 */
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

/**
 * JavaSimpleEngine has only one algorithm, and uses default preparator and
 * serving layer. Current default preparator is IdentityPreparator and serving
 * is FirstServing.
 */
class JavaSimpleEngine[TD, DP, Q, P, A](
    dataSourceClass: Class[_ <: LJavaDataSource[_ <: Params, DP, TD, Q, A]],
    preparatorClass: Class[_ <: LJavaPreparator[_ <: Params, TD, TD]],
    algorithmClassMap
      : JMap[String, Class[_ <: LJavaAlgorithm[_ <: Params, TD, _, Q, P]]],
    servingClass: Class[_ <: LJavaServing[_ <: Params, Q, P]]
) extends JavaEngine[TD, DP, TD, Q, P, A](
    dataSourceClass,
    preparatorClass,
    Map(algorithmClassMap.toSeq: _*),
    servingClass)
