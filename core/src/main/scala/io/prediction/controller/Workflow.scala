/** Copyright 2015 TappingStone, Inc.
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

import io.prediction.annotation.Experimental
import io.prediction.core.BaseEngine
import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEvaluatorResult
import io.prediction.data.storage.EvaluationInstance
import io.prediction.workflow.CoreWorkflow
import io.prediction.workflow.WorkflowUtils

/** Workflow parameters.
  *
  * @param batch Batch label of the run.
  * @param verbose Verbosity level.
  * @param saveModel Controls whether trained models are persisted.
  * @param sparkEnv Spark properties that will be set in SparkConf.setAll().
  * @param skipSanityCheck Skips all data sanity check.
  * @param stopAfterRead Stops workflow after reading from data source.
  * @param stopAfterPrepare Stops workflow after data preparation.
  * @group Workflow
  */
case class WorkflowParams(
  batch: String = "",
  verbose: Int = 2,
  saveModel: Boolean = true,
  sparkEnv: Map[String, String] =
    Map[String, String]("spark.executor.extraClassPath" -> "."),
  skipSanityCheck: Boolean = false,
  stopAfterRead: Boolean = false,
  stopAfterPrepare: Boolean = false) {
  // Temporary workaround for WorkflowParamsBuilder for Java. It doesn't support
  // custom spark environment yet.
  def this(batch: String, verbose: Int, saveModel: Boolean)
  = this(batch, verbose, saveModel, Map[String, String]())
}

/** Collection of workflow creation methods.
  * @group Workflow
  */
object Workflow {
  // evaluator is already instantiated.
  // This is an undocumented way of using evaluator. Still experimental.
  // evaluatorParams is used to write into EngineInstance, will be shown in
  // dashboard.
  /*
  def runEval[EI, Q, P, A, ER <: AnyRef](
      engine: BaseEngine[EI, Q, P, A],
      engineParams: EngineParams,
      evaluator: BaseEvaluator[EI, Q, P, A, ER],
      evaluatorParams: Params,
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      params: WorkflowParams = WorkflowParams()) {

    implicit lazy val formats = Utils.json4sDefaultFormats +
      new NameParamsSerializer

    val engineInstance = EngineInstance(
      id = "",
      status = "INIT",
      startTime = DateTime.now,
      endTime = DateTime.now,
      engineId = "",
      engineVersion = "",
      engineVariant = "",
      engineFactory = "FIXME",
      evaluatorClass = evaluator.getClass.getName(),
      batch = params.batch,
      env = env,
      sparkConf = params.sparkEnv,
      dataSourceParams = write(engineParams.dataSourceParams),
      preparatorParams = write(engineParams.preparatorParams),
      algorithmsParams = write(engineParams.algorithmParamsList),
      servingParams = write(engineParams.servingParams),
      evaluatorParams = write(evaluatorParams),
      evaluatorResults = "",
      evaluatorResultsHTML = "",
      evaluatorResultsJSON = "")

    CoreWorkflow.runEval(
      engine = engine,
      engineParams = engineParams,
      engineInstance = engineInstance,
      evaluator = evaluator,
      evaluatorParams = evaluatorParams,
      env = env,
      params = params)
  }
  */

  def runEvaluation(
      evaluation: Evaluation,
      engineParamsGenerator: EngineParamsGenerator,
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      evaluationInstance: EvaluationInstance = EvaluationInstance(),
      params: WorkflowParams = WorkflowParams()) {
    runEvaluationTypeless(
      engine = evaluation.engine,
      engineParamsList = engineParamsGenerator.engineParamsList,
      evaluationInstance = evaluationInstance,
      evaluator = evaluation.evaluator,
      env = env,
      params = params
    )
  }

  def runEvaluationTypeless[
      EI, Q, P, A, EEI, EQ, EP, EA, ER <: BaseEvaluatorResult](
      engine: BaseEngine[EI, Q, P, A],
      engineParamsList: Seq[EngineParams],
      evaluationInstance: EvaluationInstance,
      evaluator: BaseEvaluator[EEI, EQ, EP, EA, ER],
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      params: WorkflowParams = WorkflowParams()) {
    runEvaluation(
      engine = engine,
      engineParamsList = engineParamsList,
      evaluationInstance = evaluationInstance,
      evaluator = evaluator.asInstanceOf[BaseEvaluator[EI, Q, P, A, ER]],
      env = env,
      params = params)
  }

  /** :: Experimental :: */
  @Experimental
  def runEvaluation[EI, Q, P, A, R <: BaseEvaluatorResult](
      engine: BaseEngine[EI, Q, P, A],
      engineParamsList: Seq[EngineParams],
      evaluationInstance: EvaluationInstance,
      evaluator: BaseEvaluator[EI, Q, P, A, R],
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      params: WorkflowParams = WorkflowParams()) {
    CoreWorkflow.runEvaluation(
      engine = engine,
      engineParamsList = engineParamsList,
      evaluationInstance = evaluationInstance,
      evaluator = evaluator,
      env = env,
      params = params)
  }
}
