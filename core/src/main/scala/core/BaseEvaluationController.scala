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

  /*
  // Obsolete workflow scheduler code
  def prepareValidationBase(params: BaseValidationDataParams)
    : BaseValidationSeq = {
    val data = prepareValidation(params.asInstanceOf[VDP])
    new ValidationSeq[F, A](data = data)
  }
  */

  def prepareValidationBase(params: BaseValidationDataParams)
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

  def validateBase(input: (BaseFeature, BasePrediction, BaseActual))
    : BaseValidationUnit = {
    validateBase(input._1, input._2, input._3)
  }
  
  def validateBase(
      feature: BaseFeature, 
      prediction: BasePrediction, 
      actual: BaseActual)
    : BaseValidationUnit = {
    validate(
      feature.asInstanceOf[F],
      prediction.asInstanceOf[P],
      actual.asInstanceOf[A])
  }

  def validate(feature: F, predicted: P, actual: A): VU

  /*
  // obsolete code
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
  */

  def validateSetBase(
    trainingDataParams: BaseTrainingDataParams,
    validationDataParams: BaseValidationDataParams,
    validationUnits: Seq[BaseValidationUnit]): BaseValidationResults = {
    validateSet(
      trainingDataParams.asInstanceOf[TDP],
      validationDataParams.asInstanceOf[VDP],
      validationUnits.map(_.asInstanceOf[VU]))
  }

  def validateSet(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR

  /*
  // Obsolete
  def crossValidateBase(validationParamsResultsSeq: Seq[BaseValidationParamsResults])
  : BaseCrossValidationResults = {
    val input = validationParamsResultsSeq
      .map(_.asInstanceOf[ValidationParamsResults[TDP, VDP, VR]])
      .map(e => (e.trainingDataParams, e.validationDataParams, e.data))

    crossValidate(input)
  }
  */

  def crossValidateBase(
    input: Seq[(BaseTrainingDataParams, BaseValidationDataParams,
      BaseValidationResults)]): BaseCrossValidationResults = {
    crossValidate(input.map(e => (
      e._1.asInstanceOf[TDP],
      e._2.asInstanceOf[VDP],
      e._3.asInstanceOf[VR])))
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
