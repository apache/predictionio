package io.prediction.core

import io.prediction.{ 
  BaseActual, 
  BaseFeature, 
  BasePrediction,
  BaseValidationUnit,
  BaseTrainingDataParams,
  BaseValidationDataParams,
  BaseValidationResults
}

// Base Params
trait BaseParams extends AnyRef {}

// Below are internal classes used by PIO workflow
trait BasePersistentData extends AnyRef {}

trait BaseValidationSeq extends BasePersistentData {}

trait BasePredictionSeq extends BasePersistentData {}

trait BaseValidationUnitSeq extends BasePersistentData {}

trait BaseValidationParamsResults extends BasePersistentData {}

class ValidationSeq[F <: BaseFeature, A <: BaseActual](
  val data: Seq[(F, A)]) extends BaseValidationSeq {}

class PredictionSeq[F <: BaseFeature, P <: BasePrediction, A <: BaseActual](
  val data: Seq[(F, P, A)]) extends BasePredictionSeq {}

class ValidationUnitSeq[VU <: BaseValidationUnit](
  val data: Seq[VU]) extends BaseValidationUnitSeq {}

class ValidationParamsResults[
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    VR <: BaseValidationResults](
    val trainingDataParams: TDP,
    val validationDataParams: VDP,
    val data: VR) extends BaseValidationParamsResults {}



