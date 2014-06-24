package io.prediction

// FIXME(yipjustin). I am lazy...
import io.prediction.core._
    //TD <: BaseTrainingData,

trait DataPreparator[
    EDP <: BaseEvaluationDataParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD,
    F,
    A]
    extends LocalDataPreparator[EDP, TDP, VDP, TD, F, A] {
  // Data generation
  def getParamsSet(params: EDP): Seq[(TDP, VDP)]

  def prepareTraining(params: TDP): TD

  def prepareValidation(params: VDP): Seq[(F, A)]
}


trait Validator[
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    F,
    P,
    A,
    VU,
    VR,
    CVR <: AnyRef]
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
  /*
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
  */
  def apply(): BaseEvaluator[
    _ <: BaseEvaluationDataParams,
    _ <: BaseValidationParams,
    _ <: BaseTrainingDataParams,
    _ <: BaseValidationDataParams,
    _,
    _,
    _,
    _,
    _,
    _,
    _ <: AnyRef]
}

