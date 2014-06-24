package io.prediction

import io.prediction.core.BaseParams

// Evaluator Params
trait BaseEvaluationDataParams extends BaseParams {}

trait BaseTrainingDataParams extends BaseParams {}

trait BaseValidationDataParams extends BaseParams {}

trait BaseValidationParams extends BaseParams {}

// Engine Params
trait BaseCleanserParams extends BaseParams {}

trait BaseAlgoParams extends BaseParams {}

trait BaseServerParams extends BaseParams {}

// Concrete helper classes
class EmptyParams() extends BaseParams
with BaseEvaluationDataParams
with BaseTrainingDataParams
with BaseValidationDataParams
with BaseValidationParams
with BaseCleanserParams
with BaseAlgoParams
with BaseServerParams {}
