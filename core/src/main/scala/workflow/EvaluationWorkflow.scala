package io.prediction.workflow

import io.prediction.BaseAlgoParams
import io.prediction.BaseCleanserParams
import io.prediction.BaseEvaluationParams
import io.prediction.BaseModel
import io.prediction.BaseServerParams
import io.prediction.BaseTrainingDataParams
import io.prediction.core.AbstractEngine
import io.prediction.core.AbstractEvaluator
import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEvaluationSeq
import io.prediction.core.BaseEvaluationUnitSeq
import io.prediction.core.BasePersistentData
import io.prediction.core.BasePredictionSeq
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{ Map => MMap }
import scala.util.Random

object EvaluationWorkflow {
  def apply(
      batch: String,
      evalParams: BaseEvaluationParams,
      // Should group below 3 into 1
      cleanserParams: BaseCleanserParams,
      algoParamsList: Seq[(String, BaseAlgoParams)],
      serverParams: BaseServerParams,
      engine: AbstractEngine,
      evaluatorClass: Class[_ <: AbstractEvaluator]
    ): WorkflowScheduler = {
    val submitter = new WorkflowSubmitter
    // In the comment, *_id corresponds to a task id.

    val evaluator = evaluatorClass.newInstance
    
    // eval to eval_params
    val evalParamsMap = evaluator.getParamsSetBase(evalParams)
      .zipWithIndex
      .map(_.swap)
      .toMap

    // algo to algo_params
    val algoParamsMap = algoParamsList.zipWithIndex.map(_.swap).toMap

    // Data Prep
    // eval to data_prep_id
    val dataPrepMap = evalParamsMap.map{ case(eval, evalParams) => {
      val (trainDataParams, evalDataParams) = evalParams
      val task = new DataPrepTask(submitter.nextId, batch,
        evaluatorClass, trainDataParams)
      val dataPrepId = submitter.submit(task)
      (eval, dataPrepId)
    }}.toMap

    // Eval Prep
    // eval to eval_prep_id
    val evalPrepMap = evalParamsMap.map{ case(eval, evalParams) => {
      val (trainDataParams, evalDataParams) = evalParams
      val task = new EvalPrepTask(submitter.nextId, batch,
        evaluatorClass, evalDataParams)
      val evalPrepId = submitter.submit(task)
      (eval, evalPrepId)
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
      val evalPrepId = evalPrepMap(eval)
      val task = new PredictionTask(submitter.nextId, batch, engine, algoName,
        algoParams, trainingId, evalPrepId)
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

    // EvaluationUnitTask
    // eval to eval_unit_id
    val evalUnitMap = serverMap.map{ case(eval, serverId) => {
      val task = new EvaluationUnitTask(submitter.nextId, batch, 
        evaluatorClass, evalParams, serverId)
      val evalUnitId = submitter.submit(task)
      (eval, evalUnitId)
    }}

    // EvaluationReportTask
    // eval to eval_report_id
    val evalReportMap = evalUnitMap.map{ case(eval, evalUnitId) => {
      val task = new EvaluationReportTask(submitter.nextId, batch,
        evaluatorClass, evalParams, evalUnitId)
      val evalReportId = submitter.submit(task)
      (eval, evalReportId)
    }}

    return new SingleThreadScheduler()
  }
}
