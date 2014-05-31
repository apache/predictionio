package io.prediction.core

// FIXME(yipjustin). I am lazy...
import io.prediction._
import scala.reflect.Manifest


trait AbstractDataPreparator {
  // Data Preparation methods
  def getParamsSetBase(params: BaseEvaluationDataParams)
    : Seq[(BaseTrainingDataParams, BaseValidationDataParams)]
  
  def paramsClass(): Manifest[_ <: BaseEvaluationDataParams]

  def prepareTrainingBase(params: BaseTrainingDataParams)
    : BaseTrainingData

  def prepareValidationBase(params: BaseValidationDataParams)
    : BaseValidationSeq
}


trait AbstractValidator {
  // Evaluation methods
  def initBase(params: BaseValidationParams): Unit

  def paramsClass(): Manifest[_ <: BaseValidationParams]

  def validateSeq(predictionSeq: BasePredictionSeq): BaseValidationUnitSeq

  def validateSet(
    trainingDataParams: BaseTrainingDataParams,
    validationDataParams: BaseValidationDataParams,
    validationUnitSeq: BaseValidationUnitSeq)
  : BaseValidationParamsResults

  def crossValidateBase(
    validationParamsResultsSeq: Seq[BaseValidationParamsResults])
  : BaseCrossValidationResults
}

class AbstractEvaluator(
  val dataPreparatorClass: Class[_ <: AbstractDataPreparator],
  val validatorClass: Class[_ <: AbstractValidator]) {}
