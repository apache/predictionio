package io.prediction.workflow

import io.prediction.{
  AbstractEngine,
  AbstractEvaluator,
  AbstractEvaluationPreparator
}
import io.prediction.BaseTrainingDataParams
import io.prediction.BaseAlgoParams
import io.prediction.BaseServerParams
import io.prediction.BaseEvaluationDataParams
import io.prediction.BasePersistentData
import io.prediction.BaseTrainingData
import io.prediction.BaseModel
import io.prediction.BaseEvaluationSeq
import io.prediction.BasePredictionSeq
import io.prediction.BaseEvaluationUnitSeq


class Task(val id: Int, val batch: String, val dependingIds: Seq[Int]) {
  def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    null
  }
}

class DataPrepTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val dataParams: BaseTrainingDataParams
) extends Task(id, batch, Seq[Int]()) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val dataPrep = engine.dataPreparatorClass.newInstance
    dataPrep.prepareTrainingBase(dataParams)
  }
}

class EvalPrepTask(
  id: Int,
  batch: String,
  val evalPreparator: AbstractEvaluationPreparator,
  val evalDataParams: BaseEvaluationDataParams
) extends Task(id, batch, Seq[Int]()) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    evalPreparator.prepareEvaluationBase(evalDataParams)
  }
}

class TrainingTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val algoName: String,
  val algoParams: BaseAlgoParams,
  val dataPrepId: Int
) extends Task(id, batch, Seq(dataPrepId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val algorithm = engine.algorithmClassMap(algoName).newInstance
    algorithm.initBase(algoParams)
    algorithm.trainBase(input(dataPrepId).asInstanceOf[BaseTrainingData])
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
  val evaluator: AbstractEvaluator,
  val serverId: Int
) extends Task(id, batch, Seq(serverId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    evaluator.evaluateSeqBase(input(serverId).asInstanceOf[BasePredictionSeq])
  }
}

class EvaluationReportTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val evalUnitId: Int
) extends Task(id, batch, Seq(evalUnitId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    evaluator.reportBase(input(evalUnitId).asInstanceOf[BaseEvaluationUnitSeq])
  }
}
