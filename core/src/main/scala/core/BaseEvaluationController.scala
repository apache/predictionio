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
    extends AbstractDataPreparator {

  override def paramsClass() = manifest[EDP]

  override def getParamsSetBase(params: BaseEvaluationDataParams)
  : Seq[(TDP, VDP)] = getParamsSet(params.asInstanceOf[EDP])

  def getParamsSet(params: EDP): Seq[(TDP, VDP)]

  override def prepareTrainingBase(params: BaseTrainingDataParams): TD
    = prepareTraining(params.asInstanceOf[TDP])

  def prepareTraining(params: TDP): TD

  override def prepareValidationBase(params: BaseValidationDataParams)
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
    extends AbstractValidator {

  override def initBase(params: BaseValidationParams): Unit =
    init(params.asInstanceOf[VP])

  def init(params: VP): Unit = {}

  override def paramsClass() = manifest[VP]

  override def validateSeq(predictionSeq: BasePredictionSeq)
    : BaseValidationUnitSeq = {
    val input: Seq[(F, P, A)] = predictionSeq
      .asInstanceOf[PredictionSeq[F, P, A]].data
    val output = input.map(e => validate(e._1, e._2, e._3))
    return new ValidationUnitSeq(data = output)
  }

  def validate(feature: F, predicted: P, actual: A): VU

  override 
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

  def validateSet(
    trainingDataParams: TDP, 
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR

  override
  def crossValidateBase(validationParamsResultsSeq: Seq[BaseValidationParamsResults])
  : BaseCrossValidationResults = {
    val input = validationParamsResultsSeq
      .map(_.asInstanceOf[ValidationParamsResults[TDP, VDP, VR]])
      .map(e => (e.trainingDataParams, e.validationDataParams, e.data))
    
    crossValidate(input)
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
  val dataPreparatorBaseClass
    : Class[_ <: BaseDataPreparator[EDP, TDP, VDP, TD, F, A]],
  val validatorBaseClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]])
  extends AbstractEvaluator(dataPreparatorBaseClass, validatorBaseClass) {}
