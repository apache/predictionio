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

package io.prediction.workflow

import io.prediction.controller.EmptyParams
import io.prediction.controller.Engine
import io.prediction.controller.EngineParams
import io.prediction.controller.IPersistentModel
import io.prediction.controller.LAlgorithm
import io.prediction.controller.PAlgorithm
import io.prediction.controller.Metric
import io.prediction.controller.Params
import io.prediction.controller.Utils
import io.prediction.controller.NiceRendering
import io.prediction.controller.SanityCheck
/*
import io.prediction.controller.java.LJavaDataSource
import io.prediction.controller.java.LJavaPreparator
import io.prediction.controller.java.LJavaAlgorithm
import io.prediction.controller.java.LJavaServing
import io.prediction.controller.java.JavaEvaluator
import io.prediction.controller.java.JavaUtils
import io.prediction.controller.java.JavaEngine
import io.prediction.controller.java.PJavaAlgorithm
*/
import io.prediction.controller.WorkflowParams
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseDataSource
import io.prediction.core.BaseEvaluator
import io.prediction.core.BasePreparator
import io.prediction.core.BaseServing
import io.prediction.core.BaseEngine
import io.prediction.core.Doer
//import io.prediction.core.LModelAlgorithm
import io.prediction.data.storage.EngineInstance
import io.prediction.data.storage.EngineInstances
import io.prediction.data.storage.Model
import io.prediction.data.storage.Storage

import com.github.nscala_time.time.Imports.DateTime
import com.twitter.chill.KryoInjection
import grizzled.slf4j.{ Logger, Logging }
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.json4s._
import org.json4s.native.Serialization.write
import org.json4s.native.Serialization.writePretty


import scala.collection.JavaConversions._
import scala.language.existentials
import scala.reflect.ClassTag
import scala.reflect.Manifest

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.lang.{ Iterable => JIterable }
import java.util.{ HashMap => JHashMap, Map => JMap }


// CoreWorkflow handles PredictionIO metadata and environment variables.
object CoreWorkflow {
  @transient lazy val logger = Logger[this.type]
  @transient val engineInstances = Storage.getMetaDataEngineInstances

  def runTrain[EI, Q, P, A](
      engine: BaseEngine[EI, Q, P, A],
      engineParams: EngineParams,
      engineInstance: EngineInstance,
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      params: WorkflowParams = WorkflowParams()) {
    logger.debug("Starting SparkContext")
    val mode = "training"
    WorkflowUtils.checkUpgrade(mode, engineInstance.engineFactory)

    val sc = WorkflowContext(
      params.batch,
      env,
      params.sparkEnv,
      mode.capitalize)

    try {

      val models: Seq[Any] = engine.train(
        sc = sc,
        engineParams = engineParams,
        engineInstanceId = engineInstance.id,
        params = params
      )

      val instanceId = Storage.getMetaDataEngineInstances

      logger.info("Inserting persistent model")
      Storage.getModelDataModels.insert(Model(
        id = engineInstance.id,
        models = KryoInjection(models)))

      logger.info("Updating engine instance")
      val engineInstances = Storage.getMetaDataEngineInstances
      engineInstances.update(engineInstance.copy(
        status = "COMPLETED",
        endTime = DateTime.now
        ))

      logger.info("Training completed successfully.")
    } catch {
      case e @(
          _: StopAfterReadInterruption |
          _: StopAfterPrepareInterruption) => {
        logger.info(s"Training interrupted by $e.")
      }
    } finally {
      logger.debug("Stopping SparkContext")
      sc.stop()
    }
  }

  // FIXME(yipjustin): Take a Evaluator instead of Metric.
  def runEvaluation[EI, Q, P, A, R](
      engine: BaseEngine[EI, Q, P, A],
      engineParamsList: Seq[EngineParams],
      engineInstance: EngineInstance,
      //metric: Metric[EI, Q, P, A, R],
      evaluator: BaseEvaluator[EI, Q, P, A, R],
      env: Map[String, String] = WorkflowUtils.pioEnvVars,
      params: WorkflowParams = WorkflowParams()) {
    logger.info("CoreWorkflow.runEvaluation")

    logger.debug("Start spark context")

    val mode = "evaluation"
    val sc = WorkflowContext(
      params.batch,
      env,
      params.sparkEnv,
      mode.capitalize)

    WorkflowUtils.checkUpgrade(mode, engineInstance.engineFactory)
    
    val evalResult = EvaluationWorkflow.runEvaluation(
      sc,
      engine,
      engineParamsList,
      evaluator,
      //metric,
      params)

    implicit lazy val formats = Utils.json4sDefaultFormats +
      new NameParamsSerializer

    // TODO: Save best instance to EngineInstance
    val evaledEI = engineInstance.copy(
      status = "EVALCOMPLETED",
      endTime = DateTime.now,
      //evaluatorResults = bestScore.toString,
      evaluatorResults = evalResult.toString//,
      //dataSourceParams = write(bestEngineParams.dataSourceParams),
      //preparatorParams = write(bestEngineParams.preparatorParams),
      //algorithmsParams = write(bestEngineParams.algorithmParamsList),
      //servingParams = write(bestEngineParams.servingParams)
    )

    logger.info(s"Insert evaluation result")
    engineInstances.update(evaledEI)

    logger.debug("Stop spark context")
    sc.stop()

    logger.info(s"evalResult: $evalResult")

    //val bestEpJson = writePretty(bestEngineParams)

    //logger.info(s"Optimal score: $bestScore")
    //logger.info(s"Optimal engine params: $bestEpJson")

    logger.info("CoreWorkflow.runEvaluation completed.")
  }
}


