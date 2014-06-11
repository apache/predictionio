package io.prediction.core

import scala.reflect.Manifest

// FIXME(yipjustin). I am being lazy...
import io.prediction._

abstract class BaseDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    A <: BaseActual]
  extends AbstractParameterizedDoer[EDP] {

  def init(params: EDP): Unit = {}

  def getParamsSetBase(params: BaseEvaluationDataParams)
  : Seq[(TDP, VDP)] = getParamsSet(params.asInstanceOf[EDP])

  def getParamsSet(params: EDP): Seq[(TDP, VDP)]

  def prepareTrainingBase(params: BaseTrainingDataParams): TD
    = prepareTraining(params.asInstanceOf[TDP])

  def prepareTraining(params: TDP): TD

  def prepareValidationBase(params: BaseValidationDataParams)
    : BaseValidationSeq = {
    val data = prepareValidation(params.asInstanceOf[VDP])
    new ValidationSeq[F, A](data = data)
  }

  def prepareValidationSpark(params: BaseValidationDataParams)
  : Seq[(F, A)] = {
    prepareValidation(params.asInstanceOf[VDP])
  }

  def prepareValidation(params: VDP): Seq[(F, A)]
}


abstract class BaseValidator[
    VP <: BaseValidationParams : Manifest,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults]
  extends AbstractParameterizedDoer[VP] {

  def validateSeq(predictionSeq: BasePredictionSeq)
    : BaseValidationUnitSeq = {
    val input: Seq[(F, P, A)] = predictionSeq
      .asInstanceOf[PredictionSeq[F, P, A]].data
    val output = input.map(e => validate(e._1, e._2, e._3))
    return new ValidationUnitSeq(data = output)
  }

  def validateSpark(input: (BaseFeature, BasePrediction, BaseActual))
    : BaseValidationUnit = {
    validate(
      input._1.asInstanceOf[F],
      input._2.asInstanceOf[P],
      input._3.asInstanceOf[A])
  }

  def validate(feature: F, predicted: P, actual: A): VU

  def validateSet(
    trainingDataParams: BaseTrainingDataParams,
    validationDataParams: BaseValidationDataParams,
    validationUnitSeq: BaseValidationUnitSeq): BaseValidationParamsResults = {
    val tdp = trainingDataParams.asInstanceOf[TDP]
    val vdp = validationDataParams.asInstanceOf[VDP]

    val results = validateSet(tdp, vdp,
      validationUnitSeq.asInstanceOf[ValidationUnitSeq[VU]].data)

    return new ValidationParamsResults(tdp, vdp, results)
  }

  def validateSetSpark(
    input: (
      (BaseTrainingDataParams, BaseValidationDataParams),
      Iterable[BaseValidationUnit]))
    : ((BaseTrainingDataParams, BaseValidationDataParams),
      BaseValidationResults) = {
    val results = validateSet(
      input._1._1.asInstanceOf[TDP],
      input._1._2.asInstanceOf[VDP],
      input._2.map(_.asInstanceOf[VU]).toSeq)
    (input._1, results)
  }

  def validateSet(
    trainingDataParams: TDP, 
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR

  def crossValidateBase(validationParamsResultsSeq: Seq[BaseValidationParamsResults])
  : BaseCrossValidationResults = {
    val input = validationParamsResultsSeq
      .map(_.asInstanceOf[ValidationParamsResults[TDP, VDP, VR]])
      .map(e => (e.trainingDataParams, e.validationDataParams, e.data))
    
    crossValidate(input)
  }

  def crossValidateSpark(
    input: Array[
    (Int, ((BaseTrainingDataParams, BaseValidationDataParams),
      BaseValidationResults))]): BaseCrossValidationResults = {
    // maybe sort them.
    val data = input
      .map(e => (e._2._1._1, e._2._1._2, e._2._2))
      .map(e => (
        e._1.asInstanceOf[TDP],
        e._2.asInstanceOf[VDP],
        e._3.asInstanceOf[VR]))

    crossValidate(data)
  }


  def crossValidate(validateResultsSeq: Seq[(TDP, VDP, VR)]): CVR
}

/* Evaluator */
class BaseEvaluator[
    EDP <: BaseEvaluationDataParams,
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults](
  val dataPreparatorClass
    : Class[_ <: BaseDataPreparator[EDP, TDP, VDP, TD, F, A]],
  val validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]]) {}
