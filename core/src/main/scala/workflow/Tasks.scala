package io.prediction.workflow

import io.prediction.core.AbstractEngine
import io.prediction.core.AbstractEvaluator
import io.prediction.BaseTrainingDataParams
import io.prediction.BaseAlgoParams
import io.prediction.BaseCleanserParams
import io.prediction.BaseServerParams
import io.prediction.BaseEvaluationDataParams
import io.prediction.core.BasePersistentData
import io.prediction.BaseTrainingData
import io.prediction.BaseModel
import io.prediction.BaseCleansedData
import io.prediction.core.BaseEvaluationSeq
import io.prediction.core.BasePredictionSeq
import io.prediction.core.BaseEvaluationUnitSeq
import io.prediction.core.BaseEvaluator


class Task(
  val id: Int,
  val batch: String,
  val dependingIds: Seq[Int] = Seq[Int](),
  var done: Boolean = false,
  var outputPath: String = "") {

  def markDone(outputPath: String): Unit = {
    done = true
    this.outputPath = outputPath
  }
  
  def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    null
  }
  
  override def toString() = {
    (s"${this.getClass.getSimpleName} id: $id batch: $batch "
    + s"depending: $dependingIds done: $done outputPath: $outputPath")
  }
}

class DataPrepTask(
  id: Int,
  batch: String,
  val evaluatorClass: Class[_ <: AbstractEvaluator],
  val dataParams: BaseTrainingDataParams
) extends Task(id, batch, Seq[Int]()) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val evaluator = evaluatorClass.newInstance
    evaluator.prepareTrainingBase(dataParams)
  }
}

class EvalPrepTask(
  id: Int,
  batch: String,
  val evaluatorClass: Class[_ <: AbstractEvaluator],
  val evalDataParams: BaseEvaluationDataParams
) extends Task(id, batch, Seq[Int]()) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val evaluator = evaluatorClass.newInstance
    evaluator.prepareEvaluationBase(evalDataParams)
  }
}

class CleanserTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val cleanserParams: BaseCleanserParams,
  val dataPrepId: Int
) extends Task(id, batch, Seq(dataPrepId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val cleanser = engine.cleanserClass.newInstance
    cleanser.initBase(cleanserParams)
    cleanser.cleanseBase(input(dataPrepId).asInstanceOf[BaseTrainingData])
  }
  
}

class TrainingTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val algoName: String,
  val algoParams: BaseAlgoParams,
  val cleanseId: Int
) extends Task(id, batch, Seq(cleanseId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val algorithm = engine.algorithmClassMap(algoName).newInstance
    algorithm.initBase(algoParams)
    algorithm.trainBase(input(cleanseId).asInstanceOf[BaseCleansedData])
  }
}

class PredictionTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val algoName: String,
  val algoParams: BaseAlgoParams,
  val trainingId: Int,
  val evalPrepId: Int
) extends Task(id, batch, Seq(trainingId, evalPrepId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val algorithm = engine.algorithmClassMap(algoName).newInstance
    algorithm.initBase(algoParams)
    algorithm.predictSeqBase(
      baseModel = input(trainingId).asInstanceOf[BaseModel],
      evalSeq = input(evalPrepId).asInstanceOf[BaseEvaluationSeq]
    )
  }
}

class ServerTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val serverParams: BaseServerParams,
  val predictionIds: Seq[Int]
) extends Task(id, batch, predictionIds) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val server = engine.serverClass.newInstance
    server.initBase(serverParams)
    server.combineSeqBase(predictionIds.map(id =>
      input(id).asInstanceOf[BasePredictionSeq]))
  }
}

class EvaluationUnitTask(
  id: Int,
  batch: String,
  val evaluatorClass: Class[_ <: AbstractEvaluator],
  val serverId: Int
) extends Task(id, batch, Seq(serverId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val evaluator = evaluatorClass.newInstance
    evaluator.evaluateSeq(input(serverId).asInstanceOf[BasePredictionSeq])
  }
}

class EvaluationReportTask(
  id: Int,
  batch: String,
  val evaluatorClass: Class[_ <: AbstractEvaluator],
  val evalUnitId: Int
) extends Task(id, batch, Seq(evalUnitId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val evaluator = evaluatorClass.newInstance
    evaluator.report(input(evalUnitId).asInstanceOf[BaseEvaluationUnitSeq])
  }
}
