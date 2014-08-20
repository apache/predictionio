package io.prediction.controller.java;

import io.prediction.workflow.JavaAPIDebugWorkflow;
//import java.util.{ HashMap => JHashMap, Map => JMap }
import io.prediction.controller.Engine
import io.prediction.controller.EngineParams
import io.prediction.controller.WorkflowParams
import io.prediction.core.BaseMetrics
import io.prediction.controller.Params
import scala.collection.JavaConversions._

object JavaWorkflow {
  def runEngine[DP, TD, PD, Q, P, A](
    engine: Engine[TD, DP, PD, Q, P, A],
    engineParams: EngineParams,
    params: WorkflowParams
  ) {
    runEngine(
      engine = engine,
      engineParams = engineParams,
      metricsClass = null,
      metricsParams = null,
      params = params
    )
  }

  def runEngine[DP, TD, PD, Q, P, A, MU, MR, MMR <: AnyRef](
      engine: Engine[TD, DP, PD, Q, P, A],
      engineParams: EngineParams,
      metricsClass
        : Class[_ <: BaseMetrics[_ <: Params, DP, Q, P, A, MU, MR, MMR]],
      metricsParams: Params,
      params: WorkflowParams
    ) {
    JavaAPIDebugWorkflow.run(
      dataSourceClass = engine.dataSourceClass,
      dataSourceParams = engineParams.dataSourceParams,
      preparatorClass = engine.preparatorClass,
      preparatorParams = engineParams.preparatorParams,
      algorithmClassMap = engine.algorithmClassMap,
      algorithmParamsList = engineParams.algorithmParamsList,
      servingClass = engine.servingClass,
      servingParams = engineParams.servingParams,
      metricsClass = metricsClass,
      metricsParams = metricsParams,
      params = params
    )
  }
}
