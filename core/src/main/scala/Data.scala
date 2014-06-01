package io.prediction

import io.prediction.core.BasePersistentData

// Evaluator Params
trait BaseEvaluationDataParams {}

trait BaseTrainingDataParams {}

trait BaseValidationDataParams {}

trait BaseValidationParams {}

// Engine Params
trait BaseCleanserParams {}

trait BaseAlgoParams {}

trait BaseServerParams {}

class DefaultCleanserParams() extends BaseCleanserParams{}

class DefaultServerParams() extends BaseServerParams{}

// Engine Persistent Data
trait BaseCleansedData extends BasePersistentData {}

// TrainingData is a subclass of CleasedData. If an algo can handle (potentially
// unclean training data, it should also handle cleansed data.
trait BaseTrainingData extends BaseCleansedData {}

trait BaseFeature extends BasePersistentData {}

trait BasePrediction extends BasePersistentData {}

trait BaseActual extends BasePersistentData {}

trait BaseModel extends BasePersistentData {}

// Evaluator Persistent Data

trait BaseValidationUnit extends BasePersistentData {}

trait BaseValidationResults extends BasePersistentData {}

trait BaseCrossValidationResults extends BasePersistentData {}

// Concrete helper classes
class EmptyParams() extends BasePersistentData 
with BaseEvaluationDataParams
with BaseTrainingDataParams
with BaseValidationDataParams
with BaseValidationParams
with BaseCleanserParams
with BaseAlgoParams
with BaseServerParams {}

class EmptyData() extends BasePersistentData
with BaseTrainingData
with BaseFeature
with BasePrediction
with BaseActual
with BaseModel
with BaseCleansedData
with BaseValidationUnit
with BaseValidationResults
with BaseCrossValidationResults

object EmptyData {
  def apply() = new EmptyData()
}
