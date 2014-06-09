package io.prediction.core.BaseDataPreparator

trait AbstractSparkDataPreparator {

}

trait SparkDataPreparator[
  VDP <: BaseValidationDataParams,
  F <: BaseFeature,
  A <: BaseActual] {

  def prepareValidationRDDBase
  def prepareValidationRDD(params: VDP): RDD[(F, A)]

}

trait SparkValidator[
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
    validationUnits: Seq[VU]): VR = EmptyData()

  def crossValidate(validationResultsSeq: Seq[(TDP, VDP, VR)]): CVR
}
