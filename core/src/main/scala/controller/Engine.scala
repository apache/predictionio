/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.controller

import io.prediction.core.BaseDataSource
import io.prediction.core.BasePreparator
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServing

import scala.language.implicitConversions

/** This class chains up the entire data process. PredictionIO uses this
  * information to create workflows and deployments. In Scala, you should
  * implement an object that extends the `IEngineFactory` trait similar to the
  * following example.
  *
  * {{{
  * object ItemRankEngine extends IEngineFactory {
  *   def apply() = {
  *     new Engine(
  *       classOf[ItemRankDataSource],
  *       classOf[ItemRankPreparator],
  *       Map(
  *         "knn" -> classOf[KNNAlgorithm],
  *         "rand" -> classOf[RandomAlgorithm],
  *         "mahoutItemBased" -> classOf[MahoutItemBasedAlgorithm]),
  *       classOf[ItemRankServing])
  *   }
  * }
  * }}}
  *
  * @see [[IEngineFactory]]
  * @tparam TD Training data class.
  * @tparam EI Evaluation info class.
  * @tparam PD Prepared data class.
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  * @tparam A Actual value class.
  * @param dataSourceClassMap Map of data source class.
  * @param preparatorClassMap Map of preparator class.
  * @param algorithmClassMap Map of algorithm names to classes.
  * @param servingClassMap Map of serving class.
  * @group Engine
  */
class Engine[TD, EI, PD, Q, P, A](
    val dataSourceClassMap: Map[String,
      Class[_ <: BaseDataSource[TD, EI, Q, A]]],
    val preparatorClassMap: Map[String, Class[_ <: BasePreparator[TD, PD]]],
    val algorithmClassMap: Map[String, Class[_ <: BaseAlgorithm[PD, _, Q, P]]],
    val servingClassMap: Map[String, Class[_ <: BaseServing[Q, P]]])
  extends Serializable {

  /**
    * @param dataSourceClass Data source class.
    * @param preparatorClass Preparator class.
    * @param algorithmClassMap Map of algorithm names to classes.
    * @param servingClass Serving class.
    */
  def this(
    dataSourceClass: Class[_ <: BaseDataSource[TD, EI, Q, A]],
    preparatorClass: Class[_ <: BasePreparator[TD, PD]],
    algorithmClassMap: Map[String, Class[_ <: BaseAlgorithm[PD, _, Q, P]]],
    servingClass: Class[_ <: BaseServing[Q, P]]) = this(
      Map("" -> dataSourceClass),
      Map("" -> preparatorClass),
      algorithmClassMap,
      Map("" -> servingClass)
    )

  /** Returns a new Engine instnace. Mimmic case class's copy method behavior.
    */
  def copy(
    dataSourceClassMap: Map[String, Class[_ <: BaseDataSource[TD, EI, Q, A]]]
      = dataSourceClassMap,
    preparatorClassMap: Map[String, Class[_ <: BasePreparator[TD, PD]]]
      = preparatorClassMap,
    algorithmClassMap: Map[String, Class[_ <: BaseAlgorithm[PD, _, Q, P]]]
      = algorithmClassMap,
    servingClassMap: Map[String, Class[_ <: BaseServing[Q, P]]]
      = servingClassMap): Engine[TD, EI, PD, Q, P, A] = {
    new Engine(
      dataSourceClassMap,
      preparatorClassMap,
      algorithmClassMap,
      servingClassMap)
  }
}

object Engine {

  class DataSourceMap[TD, EI, Q, A](
    val m: Map[String, Class[_ <: BaseDataSource[TD, EI, Q, A]]]) {
    def this(c: Class[_ <: BaseDataSource[TD, EI, Q, A]]) = this(Map("" -> c))
  }

  object DataSourceMap {
    implicit def cToMap[TD, EI, Q, A](
      c: Class[_ <: BaseDataSource[TD, EI, Q, A]]):
      DataSourceMap[TD, EI, Q, A] = new DataSourceMap(c)
    implicit def mToMap[TD, EI, Q, A](
      m: Map[String, Class[_ <: BaseDataSource[TD, EI, Q, A]]]):
      DataSourceMap[TD, EI, Q, A] = new DataSourceMap(m)
  }

  class PreparatorMap[TD, PD](
    val m: Map[String, Class[_ <: BasePreparator[TD, PD]]]) {
    def this(c: Class[_ <: BasePreparator[TD, PD]]) = this(Map("" -> c))
  }

  object PreparatorMap {
    implicit def cToMap[TD, PD](
      c: Class[_ <: BasePreparator[TD, PD]]):
      PreparatorMap[TD, PD] = new PreparatorMap(c)
    implicit def mToMap[TD, PD](
      m: Map[String, Class[_ <: BasePreparator[TD, PD]]]):
      PreparatorMap[TD, PD] = new PreparatorMap(m)
  }

  class ServingMap[Q, P](
    val m: Map[String, Class[_ <: BaseServing[Q, P]]]) {
    def this(c: Class[_ <: BaseServing[Q, P]]) = this(Map("" -> c))
  }

  object ServingMap {
    implicit def cToMap[Q, P](
      c: Class[_ <: BaseServing[Q, P]]): ServingMap[Q, P] =
        new ServingMap(c)
    implicit def mToMap[Q, P](
      m: Map[String, Class[_ <: BaseServing[Q, P]]]): ServingMap[Q, P] =
        new ServingMap(m)
  }

  def apply[TD, EI, PD, Q, P, A](
    dataSourceMap: DataSourceMap[TD, EI, Q, A],
    preparatorMap: PreparatorMap[TD, PD],
    algorithmClassMap: Map[String, Class[_ <: BaseAlgorithm[PD, _, Q, P]]],
    servingMap: ServingMap[Q, P]) = new Engine(
      dataSourceMap.m,
      preparatorMap.m,
      algorithmClassMap,
      servingMap.m
    )

}
/** This class serves as a logical grouping of all required engine's parameters.
  *
  * @param dataSourceParams Data Source name-parameters tuple.
  * @param preparatorParams Preparator name-parameters tuple.
  * @param algorithmParamsList List of algorithm name-parameter pairs.
  * @param servingParams Serving name-parameters tuple.
  * @group Engine
  */
class EngineParams(
    val dataSourceParams: (String, Params) = ("", EmptyParams()),
    val preparatorParams: (String, Params) = ("", EmptyParams()),
    val algorithmParamsList: Seq[(String, Params)] = Seq(),
    val servingParams: (String, Params) = ("", EmptyParams()))
  extends Serializable {}

object EngineParams {
  /** Create EngineParams
    * @param dataSourceName Data Soure name
    * @param dataSourceParams Data Soure parameters
    * @param preparatorName Preparator name
    * @param preparatorParams Preparator parameters
    * @param algorithmParamsList List of algorithm name-parameter pairs.
    * @param servingName Serving name
    * @param servingParams Serving parameters
    * @group Engine
    */
  def apply(
    dataSourceName: String = "",
    dataSourceParams: Params = EmptyParams(),
    preparatorName: String = "",
    preparatorParams: Params = EmptyParams(),
    algorithmParamsList: Seq[(String, Params)] = Seq(),
    servingName: String = "",
    servingParams: Params = EmptyParams()): EngineParams = {
      new EngineParams(
        dataSourceParams = (dataSourceName, dataSourceParams),
        preparatorParams = (preparatorName, preparatorParams),
        algorithmParamsList = algorithmParamsList,
        servingParams = (servingName, servingParams)
      )
    }
}

/** SimpleEngine has only one algorithm, and uses default preparator and serving
  * layer. Current default preparator is `IdentityPreparator` and serving is
  * `FirstServing`.
  *
  * @tparam TD Training data class.
  * @tparam EI Evaluation info class.
  * @tparam PD Prepared data class.
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  * @tparam A Actual value class.
  * @param dataSourceClass Data source class.
  * @param algorithmClassMap Map of algorithm names to classes.
  * @group Engine
  */
class SimpleEngine[TD, EI, Q, P, A](
    dataSourceClass: Class[_ <: BaseDataSource[TD, EI, Q, A]],
    algorithmClass: Class[_ <: BaseAlgorithm[TD, _, Q, P]])
  extends Engine(
    dataSourceClass,
    IdentityPreparator(dataSourceClass),
    Map("" -> algorithmClass),
    LFirstServing(algorithmClass))

/** This shorthand class serves the `SimpleEngine` class.
  *
  * @param dataSourceParams Data source parameters.
  * @param algorithmParamsList List of algorithm name-parameter pairs.
  * @group Engine
  */
class SimpleEngineParams(
    dataSourceParams: Params = EmptyParams(),
    algorithmParams: Params = EmptyParams())
  extends EngineParams(
    dataSourceParams = ("", dataSourceParams),
    algorithmParamsList = Seq(("", algorithmParams)))

/** If you intend to let PredictionIO create workflow and deploy serving
  * automatically, you will need to implement an object that extends this trait
  * and return an [[Engine]].
  *
  * @group Engine
  */
trait IEngineFactory {
  /** Creates an instance of an [[Engine]]. */
  def apply(): Engine[_, _, _, _, _, _]

  /** Override this method to programatically return engine parameters. */
  def engineParams(key: String): EngineParams = EngineParams()
}

/** Mix in this trait for queries that contain prId (PredictedResultId).
  * This is useful when your engine expects queries to also be associated with
  * prId keys when feedback loop is enabled.
  *
  * @group General
  */
trait WithPrId {
  val prId: String = ""
}
