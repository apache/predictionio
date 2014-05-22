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


class Task(val id: Int, val batch: String, val dependingIds: Seq[Int]) {}

class DataPrepTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val dataParams: BaseTrainingDataParams
) extends Task(id, batch, Seq[Int]()) {}

class EvalPrepTask(
  id: Int,
  batch: String,
  val evalPreparator: AbstractEvaluationPreparator,
  val evalDataParam: BaseEvaluationDataParams
) extends Task(id, batch, Seq[Int]()) {}

class TrainingTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val algoName: String,
  val algoParams: BaseAlgoParams,
  val dataPrepId: Int
) extends Task(id, batch, Seq(dataPrepId)) {}

class PredictionTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val algoName: String,
  val algoParams: BaseAlgoParams,
  val trainingId: Int,
  val evalPrepId: Int
) extends Task(id, batch, Seq(trainingId, evalPrepId)) {}

class ServerTask(
  id: Int,
  batch: String,
  val engine: AbstractEngine,
  val serverParams: BaseServerParams,
  val predictionIds: Seq[Int]
) extends Task(id, batch, predictionIds) {}

class EvaluationUnitTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val serverId: Int,
  val evalPrepId: Int
) extends Task(id, batch, Seq(serverId, evalPrepId)) {}

class EvaluationReportTask(
  id: Int,
  batch: String,
  val evaluator: AbstractEvaluator,
  val evalUnitId: Int
) extends Task(id, batch, Seq(evalUnitId)) {}
