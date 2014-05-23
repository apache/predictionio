package io.prediction.workflow

import io.prediction.BaseAlgoParams
import io.prediction.BaseCleanserParams
import io.prediction.BaseEvaluationParams
import io.prediction.BaseModel
import io.prediction.BaseServerParams
import io.prediction.BaseTrainingDataParams
import io.prediction.core.AbstractEngine
import io.prediction.core.AbstractEvaluator
import io.prediction.core.BaseEvaluationSeq
import io.prediction.core.BaseEvaluationUnitSeq
import io.prediction.core.BasePersistentData
import io.prediction.core.BasePredictionSeq
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{ Map => MMap }
import scala.util.Random

class Workflow(val batch: String = "") {
  val tasks: ArrayBuffer[Task] = ArrayBuffer[Task]()
  var _lastId: Int = -1
  def nextId() : Int = { _lastId += 1; _lastId }

  def submit(task: Task): Int = {
    tasks.append(task)
    println(s"Task id: ${task.id} depends: ${task.dependingIds} task: $task")
    task.id
  }

  def runTask(task: Task, dataMap: MMap[Int, BasePersistentData]): Unit = {
    // gather dependency
    val localDataMap = task.dependingIds.map(id => (id, dataMap(id))).toMap
    val taskOutput = task.run(localDataMap)
    dataMap += (task.id -> taskOutput)
    println(task.id)
    println(taskOutput)
  }

  // This function serves as a simple single threaded workflow scheduler.
  def run(): Unit = {
    val doneTaskMap: MMap[Int, Boolean] 
      = MMap(this.tasks.map(task => (task.id, false)) : _*)

    val dataMap = MMap[Int, BasePersistentData]()

    // Shuffle the task just to make sure we are handling the dependence right.
    val tasks = Random.shuffle(this.tasks)
    println(tasks.map(_.id))

    var changed = false
    do {
      changed = false

      // Find one task
      val taskOpt = tasks.find(t => {
        !doneTaskMap(t.id) && t.dependingIds.map(doneTaskMap).fold(true)(_ && _)
      })

      taskOpt.map{ task => { 
        println(s"Task: ${task.id} $task")
        runTask(task, dataMap)

        changed = true
        doneTaskMap(task.id) = true
      }}
    } while (changed)

    println(doneTaskMap)
    
  }
}

object EvaluationWorkflow {
  def apply(
      batch: String,
      evalParams: BaseEvaluationParams,
      // Should group below 3 into 1
      cleanserParams: BaseCleanserParams,
      algoParamsList: Seq[(String, BaseAlgoParams)],
      serverParams: BaseServerParams,
      engine: AbstractEngine,
      evaluator: AbstractEvaluator): Workflow = {
    val workflow = new Workflow(batch)
    // In the comment, *_id corresponds to a task id.
    
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
      val task = new DataPrepTask(workflow.nextId, batch,
        evaluator, trainDataParams)
      val dataPrepId = workflow.submit(task)
      (eval, dataPrepId)
    }}.toMap

    // Eval Prep
    // eval to eval_prep_id
    val evalPrepMap = evalParamsMap.map{ case(eval, evalParams) => {
      val (trainDataParams, evalDataParams) = evalParams
      val task = new EvalPrepTask(workflow.nextId, batch,
        evaluator, evalDataParams)
      val evalPrepId = workflow.submit(task)
      (eval, evalPrepId)
    }}.toMap

    // Cleansing
    // eval to cleansing_id
    val cleanserMap = dataPrepMap.map{ case(eval, dataPrepId) => {
      val task = new CleanserTask(workflow.nextId, batch, engine,
        cleanserParams, dataPrepId)
      val cleanserId = workflow.submit(task)
      (eval, cleanserId)
    }}

    // Training
    // (eval, algo) to training_id
    val modelMap = cleanserMap.map{ case(eval, cleanserId) => {
      algoParamsMap.map{ case(algo, (algoName, algoParams)) => {
        val task = new TrainingTask(workflow.nextId, batch, 
          engine, algoName, algoParams, cleanserId)
        val trainingId = workflow.submit(task)
        ((eval, algo) , trainingId)
      }}
    }}.flatten.toMap

    // Prediction Task
    // (eval, algo) to predict_id
    val predictionMap = modelMap.map{ case((eval, algo), trainingId) => {
      val (algoName, algoParams) = algoParamsMap(algo)
      val evalPrepId = evalPrepMap(eval)
      val task = new PredictionTask(workflow.nextId, batch, engine, algoName,
        algoParams, trainingId, evalPrepId)
      val predictionId = workflow.submit(task)
      ((eval, algo), predictionId)
    }}.toMap

    // Server Task
    // eval to server_id, (prediction task group by eval)
    val algoList = algoParamsMap.keys.toSeq
    val serverMap = evalParamsMap.map{ case(eval, _) => {
      val predictionIds = algoList.map(algo => predictionMap((eval, algo)))
      val task = new ServerTask(workflow.nextId, batch, engine, serverParams,
        predictionIds)
      val serverId = workflow.submit(task)
      (eval, serverId)
    }}.toMap

    // EvaluationUnitTask
    // eval to eval_unit_id
    val evalUnitMap = serverMap.map{ case(eval, serverId) => {
      val task = new EvaluationUnitTask(workflow.nextId, batch, evaluator,
        serverId)
      val evalUnitId = workflow.submit(task)
      (eval, evalUnitId)
    }}

    // EvaluationReportTask
    // eval to eval_report_id
    val evalReportMap = evalUnitMap.map{ case(eval, evalUnitId) => {
      val task = new EvaluationReportTask(workflow.nextId, batch, evaluator,
        evalUnitId)
      val evalReportId = workflow.submit(task)
      (eval, evalReportId)
    }}

    return workflow
  }
}
