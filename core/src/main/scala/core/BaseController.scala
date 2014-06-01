package io.prediction.core

import scala.reflect.Manifest

// FIXME(yipjustin). I am being lazy...
import io.prediction._

abstract class BaseCleanser[
    -TD <: BaseTrainingData,
    +CD <: BaseCleansedData,
    CP <: BaseCleanserParams: Manifest]
  extends AbstractCleanser {

  override def initBase(params: BaseCleanserParams): Unit = {
    init(params.asInstanceOf[CP])
  }

  def init(params: CP): Unit

  override def paramsClass() = manifest[CP]

  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
    cleanse(trainingData.asInstanceOf[TD])
  }

  def cleanse(trainingData: TD): CD
}

/* Algorithm */

abstract class BaseAlgorithm[
    -CD <: BaseCleansedData,
    -F <: BaseFeature,
    +P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams: Manifest]
  extends AbstractAlgorithm {

  override def initBase(baseAlgoParams: BaseAlgoParams): Unit =
    init(baseAlgoParams.asInstanceOf[AP])

  def init(algoParams: AP): Unit = {}

  override def paramsClass() = manifest[AP]

  override def trainBase(cleansedData: BaseCleansedData): BaseModel =
    train(cleansedData.asInstanceOf[CD])

  def train(cleansedData: CD): M

  override def predictSeqBase(baseModel: BaseModel,
    validationSeq: BaseValidationSeq): BasePredictionSeq = {

    val input: Seq[(F, BaseActual)] = validationSeq
      .asInstanceOf[ValidationSeq[F, BaseActual]]
      .data

    val model = baseModel.asInstanceOf[M]
    // Algorithm don't know the actual subtype used.
    val output: Seq[(F, P, BaseActual)] = input.map{ case(f, a) => {
      (f, predict(model, f), a)
    }}
    new PredictionSeq[F, P, BaseActual](data = output)
  }

  def predict(model: M, feature: F): P

}

/* Server */

abstract class BaseServer[
    -F <: BaseFeature,
    P <: BasePrediction,
    SP <: BaseServerParams: Manifest]
    extends AbstractServer {

  override def initBase(baseServerParams: BaseServerParams): Unit =
    init(baseServerParams.asInstanceOf[SP])

  override def paramsClass() = manifest[SP]

  def init(serverParams: SP): Unit = {}

  def combineSeqBase(basePredictionSeqSeq: Seq[BasePredictionSeq])
    : BasePredictionSeq = {
    val dataSeq: Seq[Seq[(F, P, BaseActual)]] = basePredictionSeqSeq
      .map(_.asInstanceOf[PredictionSeq[F, P, BaseActual]].data).transpose

    val output: Seq[(F, P, BaseActual)] = dataSeq.map{ input => {
      val f = input(0)._1
      val ps = input.map(_._2)
      val a = input(0)._3
      // TODO(yipjustin). Check all seqs have the same f and a
      val p = combine(f, ps)
      (f, p, a)
    }}
    new PredictionSeq[F, P, BaseActual](data = output)
  }

  def combine(feature: F, predictions: Seq[P]): P

}

/* Engine */

class BaseEngine[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction](
    cleanserClass: Class[_ <: BaseCleanser[TD, CD, _ <: BaseCleanserParams]],
    algorithmClassMap:
      Map[String,
        Class[_ <:
          BaseAlgorithm[CD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
  extends AbstractEngine(cleanserClass, algorithmClassMap, serverClass) {
}
