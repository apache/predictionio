package io.prediction

// FIXME(yipjustin). I am lazy...
import io.prediction.core._

trait DataPreparator[
    EDP <: BaseParams,
    TDP <: BaseParams,
    VDP <: BaseParams,
    TD, F, A]
  extends LocalDataPreparator[EDP, TDP, VDP, TD, F, A] {
  // Data generation
  def getParamsSet(params: EDP): Seq[(TDP, VDP)]

  def prepareTraining(params: TDP): TD

  def prepareValidation(params: VDP): Seq[(F, A)]
}


trait Validator[
    VP <: BaseParams,
    TDP <: BaseParams,
    VDP <: BaseParams,
    F, P, A, VU, VR, CVR <: AnyRef]
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
  def apply(): BaseEvaluator[
    _ <: BaseParams,
    _ <: BaseParams,
    _ <: BaseParams,
    _ <: BaseParams,
    _, _, _, _, _, _, _ <: AnyRef]
}

