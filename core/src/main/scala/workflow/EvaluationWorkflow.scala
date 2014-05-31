package io.prediction.workflow

import io.prediction.BaseAlgoParams
import io.prediction.BaseCleanserParams
import io.prediction.BaseEvaluationDataParams
import io.prediction.BaseValidationParams
import io.prediction._
import io.prediction.BaseModel
import io.prediction.BaseServerParams
import io.prediction.BaseTrainingDataParams
import io.prediction.core.AbstractEngine
import io.prediction.core.AbstractEvaluator
import io.prediction.core.BaseEvaluator
import io.prediction.core.BasePersistentData
import io.prediction.core.BasePredictionSeq
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{ Map => MMap }
import scala.util.Random

object EvaluationWorkflow {
  def apply(
      batch: String,
      // Should group below 2 into 1
      evalDataParams: BaseEvaluationDataParams,
      validationParams: BaseValidationParams,
      // Should group below 3 into 1
      cleanserParams: BaseCleanserParams,
      algoParamsList: Seq[(String, BaseAlgoParams)],
      serverParams: BaseServerParams,
      engine: AbstractEngine,
      evaluator: AbstractEvaluator
    ): WorkflowScheduler = {
    val submitter = new WorkflowSubmitter
    // In the comment, *_id corresponds to a task id.

    val dataPrep = evaluator.dataPreparatorClass.newInstance

    // eval to eval_params
    val evalParamsMap = dataPrep.getParamsSetBase(evalDataParams)
      .zipWithIndex
      .map(_.swap)
      .toMap

    // algo to algo_params
    val algoParamsMap = algoParamsList.zipWithIndex.map(_.swap).toMap

    // Data Prep
    // eval to data_prep_id
    val dataPrepMap = evalParamsMap.map{ case(eval, evalParams) => {
      val (trainDataParams, _) = evalParams
      val task = new DataPrepTask(submitter.nextId, batch,
        evaluator, trainDataParams)
      val dataPrepId = submitter.submit(task)
      (eval, dataPrepId)
    }}.toMap

    // Eval Prep
    // eval to validation_prep_id
    val validationPrepMap = evalParamsMap.map{ case(eval, evalParams) => {
      val (_, validationDataParams) = evalParams
      val task = new ValidationPrepTask(submitter.nextId, batch,
        evaluator, validationDataParams)
      val validationPrepId = submitter.submit(task)
      (eval, validationPrepId)
    }}.toMap

    // Cleansing
    // eval to cleansing_id
    val cleanserMap = dataPrepMap.map{ case(eval, dataPrepId) => {
      val task = new CleanserTask(submitter.nextId, batch, engine,
        cleanserParams, dataPrepId)
      val cleanserId = submitter.submit(task)
      (eval, cleanserId)
    }}

    // Training
    // (eval, algo) to training_id
    val modelMap = cleanserMap.map{ case(eval, cleanserId) => {
      algoParamsMap.map{ case(algo, (algoName, algoParams)) => {
        val task = new TrainingTask(submitter.nextId, batch, 
          engine, algoName, algoParams, cleanserId)
        val trainingId = submitter.submit(task)
        ((eval, algo) , trainingId)
      }}
    }}.flatten.toMap

    // Prediction Task
    // (eval, algo) to predict_id
    val predictionMap = modelMap.map{ case((eval, algo), trainingId) => {
      val (algoName, algoParams) = algoParamsMap(algo)
      val validationPrepId = validationPrepMap(eval)
      val task = new PredictionTask(submitter.nextId, batch, engine, algoName,
        algoParams, trainingId, validationPrepId)
      val predictionId = submitter.submit(task)
      ((eval, algo), predictionId)
    }}.toMap

    // Server Task
    // eval to server_id, (prediction task group by eval)
    val algoList = algoParamsMap.keys.toSeq
    val serverMap = evalParamsMap.map{ case(eval, _) => {
      val predictionIds = algoList.map(algo => predictionMap((eval, algo)))
      val task = new ServerTask(submitter.nextId, batch, engine, serverParams,
        predictionIds)
      val serverId = submitter.submit(task)
      (eval, serverId)
    }}.toMap

    // ValidationUnitTask
    // eval to validation_unit_id
    val validationUnitMap = serverMap.map{ case(eval, serverId) => {
      val task = new ValidationUnitTask(submitter.nextId, batch, 
        evaluator, validationParams, serverId)
      val validationUnitId = submitter.submit(task)
      (eval, validationUnitId)
    }}

    // ValidationSetTask
    // eval to validation_set_id
    val validationSetMap = validationUnitMap.map{ 
      case(eval, validationUnitId) => {
        val (trainDataParams, validationDataParams) = evalParamsMap(eval)
        val task = new ValidationSetTask(submitter.nextId, batch,
          evaluator, 
          validationParams, 
          trainDataParams, validationDataParams,
          validationUnitId)
        val validationSetId = submitter.submit(task)
        (eval, validationSetId)
    }}

    // CrossValidationTask
    val task = new CrossValidationTask(submitter.nextId, batch,
      evaluator,
      validationParams,
      validationSetMap.values.toList)
    val crossValidationId = submitter.submit(task)

    // FIXME(yipjustin). Workflow should not return a schduler.
    // Schduler should work independently.
    return new SingleThreadScheduler()
  }
}
