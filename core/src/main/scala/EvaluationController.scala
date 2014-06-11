package io.prediction

// FIXME(yipjustin). I am lazy...
import io.prediction.core._

trait DataPreparator[
    EDP <: BaseEvaluationDataParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    A <: BaseActual]
    extends BaseDataPreparator[EDP, TDP, VDP, TD, F, A] {
  // Data generation
  def getParamsSet(params: EDP): Seq[(TDP, VDP)]

  def prepareTraining(params: TDP): TD

  def prepareValidation(params: VDP): Seq[(F, A)]
  
}


trait Validator[
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults]
    extends BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR] {
  def init(params: VP): Unit

  def validate(feature: F, predicted: P, actual: A): VU

  def validateSet(
    trainingDataParams: TDP, 
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR

  def crossValidate(validationResultsSeq: Seq[(TDP, VDP, VR)]): CVR
}

// Factory Methods
trait EvaluatorFactory {
  //def apply(): AbstractEvaluator
  def apply(): BaseEvaluator[
    _ <: BaseEvaluationDataParams,
    _ <: BaseValidationParams,
    _ <: BaseTrainingDataParams,
    _ <: BaseValidationDataParams,
    _ <: BaseTrainingData,
    _ <: BaseFeature,
    _ <: BasePrediction,
    _ <: BaseActual,
    _ <: BaseValidationUnit,
    _ <: BaseValidationResults,
    _ <: BaseCrossValidationResults]
}

