package n.io.prediction.controller

import n.io.prediction.core.BaseEngine
import n.io.prediction.core.BaseDataSource
import n.io.prediction.core.BasePreparator
import n.io.prediction.core.BaseAlgorithm
import n.io.prediction.core.BaseServing
import n.io.prediction.workflow.EngineWorkflow
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import io.prediction.core.Doer
import io.prediction.controller.EngineParams

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
  extends BaseEngine[EI, Q, P, A] {

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
  
  def train(sc: SparkContext, engineParams: EngineParams): Seq[Any] = {
    val (dataSourceName, dataSourceParams) = engineParams.dataSourceParams
    val dataSource = Doer(dataSourceClassMap(dataSourceName), dataSourceParams)
    
    val (preparatorName, preparatorParams) = engineParams.preparatorParams
    val preparator = Doer(preparatorClassMap(preparatorName), preparatorParams)
 
    val algoParamsList = engineParams.algorithmParamsList

    val algorithms = algoParamsList.map { case (algoName, algoParams) => {
      Doer(algorithmClassMap(algoName), algoParams)
    }}

    EngineWorkflow.train(sc, dataSource, preparator, algorithms)
  }

  def eval(sc: SparkContext, engineParams: EngineParams)
  : Seq[(EI, RDD[(Q, P, A)])] = {
    val (dataSourceName, dataSourceParams) = engineParams.dataSourceParams
    val dataSource = Doer(dataSourceClassMap(dataSourceName), dataSourceParams)
    
    val (preparatorName, preparatorParams) = engineParams.preparatorParams
    val preparator = Doer(preparatorClassMap(preparatorName), preparatorParams)
 
    val algoParamsList = engineParams.algorithmParamsList

    val algorithms = algoParamsList.map { case (algoName, algoParams) => {
      Doer(algorithmClassMap(algoName), algoParams)
    }}

    val (servingName, servingParams) = engineParams.servingParams
    val serving = Doer(servingClassMap(servingName), servingParams)
    
    EngineWorkflow.eval(sc, dataSource, preparator, algorithms, serving)
  }
}


