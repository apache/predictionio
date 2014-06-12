package io.prediction.core

import scala.reflect.Manifest

// FIXME(yipjustin). I am being lazy...
import io.prediction._

abstract class BaseCleanser[
    -TD <: BaseTrainingData,
    +CD <: BaseCleansedData,
    CP <: BaseCleanserParams: Manifest]
  extends AbstractParameterizedDoer[CP] {

  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
    cleanse(trainingData.asInstanceOf[TD])
  }

  def cleanse(trainingData: TD): CD
}

/* Algorithm */

abstract class BaseAlgorithm[
    -CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams: Manifest]
  extends AbstractParameterizedDoer[AP] {

  def trainBase(cleansedData: BaseCleansedData): BaseModel =
    train(cleansedData.asInstanceOf[CD])

  def train(cleansedData: CD): M

  def predictSeqBase(baseModel: BaseModel,
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

  def predictBase(baseModel: BaseModel, baseFeature: BaseFeature)
    : BasePrediction = {
    predict(
      baseModel.asInstanceOf[M],
      baseFeature.asInstanceOf[F])
  }

  def predict(model: M, feature: F): P

}

/* Server */

abstract class BaseServer[
    -F <: BaseFeature,
    P <: BasePrediction,
    SP <: BaseServerParams: Manifest]
  extends AbstractParameterizedDoer[SP] {

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

  def combineBase(
    baseFeature: BaseFeature,
    basePredictions: Seq[BasePrediction])
    : BasePrediction = {
    combine(
      baseFeature.asInstanceOf[F],
      basePredictions.map(_.asInstanceOf[P]))
  }

  def combine(feature: F, predictions: Seq[P]): P

}

/* Engine */

class BaseEngine[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction](
    val cleanserClass
      : Class[_ <: BaseCleanser[TD, CD, _ <: BaseCleanserParams]],
    val algorithmClassMap
      : Map[String,
        Class[_ <:
          BaseAlgorithm[CD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    val serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]]) {}
