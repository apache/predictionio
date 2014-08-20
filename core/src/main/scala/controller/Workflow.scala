package io.prediction.controller

import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseDataSource
import io.prediction.core.BaseMetrics
import io.prediction.core.BasePreparator
import io.prediction.core.BaseServing
import io.prediction.core.Doer
import io.prediction.core.LModelAlgorithm
import io.prediction.workflow.WorkflowUtils
import io.prediction.workflow.APIDebugWorkflow
import scala.reflect.ClassTag

object Workflow {
  def runEngine[
      DP, TD, PD, Q, P, A,
      MU : ClassTag, MR : ClassTag, MMR <: AnyRef :ClassTag
      ](
      batch: String = "",
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      verbose: Int = 2,
      engine: Engine[TD, DP, PD, Q, P, A],
      engineParams: EngineParams,
      metricsClassOpt
        : Option[Class[_ <: BaseMetrics[_ <: Params, DP, Q, P, A, MU, MR, MMR]]]
        = None,
      metricsParams: Params = EmptyParams()) {

    run(
      batch = batch,
      env = env,
      verbose = verbose,
      dataSourceClassOpt = Some(engine.dataSourceClass),
      dataSourceParams = engineParams.dataSourceParams,
      preparatorClassOpt = Some(engine.preparatorClass),
      preparatorParams = engineParams.preparatorParams,
      algorithmClassMapOpt = Some(engine.algorithmClassMap),
      algorithmParamsList = engineParams.algorithmParamsList,
      servingClassOpt = Some(engine.servingClass),
      servingParams = engineParams.servingParams,
      metricsClassOpt = metricsClassOpt,
      metricsParams = metricsParams
    )
  }

  def run[
      DP, TD, PD, Q, P, A,
      MU : ClassTag, MR : ClassTag, MMR <: AnyRef :ClassTag
      ](
      batch: String = "",
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      verbose: Int = 2,
      dataSourceClassOpt
        : Option[Class[_ <: BaseDataSource[_ <: Params, DP, TD, Q, A]]] = None,
      dataSourceParams: Params = EmptyParams(),
      preparatorClassOpt
        : Option[Class[_ <: BasePreparator[_ <: Params, TD, PD]]] = None,
      preparatorParams: Params = EmptyParams(),
      algorithmClassMapOpt
        : Option[Map[String, Class[_ <: BaseAlgorithm[_ <: Params, PD, _, Q, P]]]]
        = None,
      algorithmParamsList: Seq[(String, Params)] = null,
      servingClassOpt: Option[Class[_ <: BaseServing[_ <: Params, Q, P]]]
        = None,
      servingParams: Params = EmptyParams(),
      metricsClassOpt
        : Option[Class[_ <: BaseMetrics[_ <: Params, DP, Q, P, A, MU, MR, MMR]]]
        = None,
      metricsParams: Params = EmptyParams()) {

    APIDebugWorkflow.runTypeless(
        batch, env, verbose,
        dataSourceClassOpt, dataSourceParams,
        preparatorClassOpt, preparatorParams,
        algorithmClassMapOpt, algorithmParamsList,
        servingClassOpt, servingParams,
        metricsClassOpt, metricsParams)
  }

}

