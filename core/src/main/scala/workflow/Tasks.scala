package io.prediction.workflow

import io.prediction.core.AbstractEngine
import io.prediction.core.AbstractEvaluator
import io.prediction.BaseTrainingDataParams
import io.prediction.BaseEvaluationDataParams
import io.prediction.BaseValidationDataParams
import io.prediction.BaseValidationParams
import io.prediction.BaseAlgoParams
import io.prediction.BaseCleanserParams
import io.prediction.BaseServerParams
import io.prediction.BaseEvaluationDataParams
import io.prediction.core.BasePersistentData
import io.prediction.BaseTrainingData
import io.prediction.BaseModel
import io.prediction.BaseCleansedData
import io.prediction.core.BaseValidationSeq
import io.prediction.core.BasePredictionSeq
import io.prediction.core.BaseValidationUnitSeq
import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseValidationParamsResults


abstract class Task(
  val id: Int,
  val batch: String,
  val dependingIds: Seq[Int] = Seq[Int](),
  var done: Boolean = false,
  var outputPath: String = "") {

  def markDone(outputPath: String): Unit = {
    done = true
    this.outputPath = outputPath
  }
  
  def run(input: Map[Int, BasePersistentData]): BasePersistentData
  
  override def toString() = {
    (s"${this.getClass.getSimpleName} id: $id batch: $batch "
    + s"depending: $dependingIds done: $done outputPath: $outputPath")
  }
}

class DataPrepTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val dataParams: BaseTrainingDataParams
) extends Task(id, batch, Seq[Int]()) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val dataPreparator = evaluator.dataPreparatorClass.newInstance
    dataPreparator.prepareTrainingBase(dataParams)
  }
}

class ValidationPrepTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val validationDataParams: BaseValidationDataParams
) extends Task(id, batch, Seq[Int]()) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val dataPreparator = evaluator.dataPreparatorClass.newInstance
    dataPreparator.prepareValidationBase(validationDataParams)
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
  val validationPrepId: Int
) extends Task(id, batch, Seq(trainingId, validationPrepId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val algorithm = engine.algorithmClassMap(algoName).newInstance
    algorithm.initBase(algoParams)
    algorithm.predictSeqBase(
      baseModel = input(trainingId).asInstanceOf[BaseModel],
      validationSeq = input(validationPrepId).asInstanceOf[BaseValidationSeq]
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

class ValidationUnitTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val validationParams: BaseValidationParams,
  val serverId: Int
) extends Task(id, batch, Seq(serverId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val validator = evaluator.validatorClass.newInstance
    validator.initBase(validationParams)
    validator.validateSeq(input(serverId).asInstanceOf[BasePredictionSeq])
  }
}

class ValidationSetTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val validationParams: BaseValidationParams,
  val trainingDataParams: BaseTrainingDataParams,
  val validationDataParams: BaseValidationDataParams,
  val validationUnitId: Int
) extends Task(id, batch, Seq(validationUnitId)) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val validator = evaluator.validatorClass.newInstance
    validator.initBase(validationParams)
    validator.validateSet(
      trainingDataParams,
      validationDataParams,
      input(validationUnitId).asInstanceOf[BaseValidationUnitSeq])
  }
}

class CrossValidationTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val validationParams: BaseValidationParams,
  val validationSetIds: Seq[Int]
) extends Task(id, batch, validationSetIds) {
  override def run(input: Map[Int, BasePersistentData]): BasePersistentData = {
    val validator = evaluator.validatorClass.newInstance
    validator.initBase(validationParams)
    validator.crossValidateBase(
      validationSetIds.map(id =>
        input(id).asInstanceOf[BaseValidationParamsResults]))
  }
}
