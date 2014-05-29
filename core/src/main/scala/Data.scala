package io.prediction

import io.prediction.core.BasePersistentData

trait BaseEvaluationParams {}

trait BaseTrainingDataParams {}

trait BaseEvaluationDataParams {}

trait BaseEvaluationUnit extends BasePersistentData {}

trait BaseEvaluationResults extends BasePersistentData {}

// Params
trait BaseCleanserParams {}

trait BaseAlgoParams {}

trait BaseServerParams {}

class DefaultCleanserParams() extends BaseCleanserParams{}

class DefaultServerParams() extends BaseServerParams{}

// Persistent Data
trait BaseCleansedData extends BasePersistentData {}

// TrainingData is a subclass of CleasedData. If an algo can handle (potentially
// unclean training data, it should also handle cleansed data.
trait BaseTrainingData extends BaseCleansedData {}

trait BaseFeature extends BasePersistentData {}

trait BasePrediction extends BasePersistentData {}

trait BaseActual extends BasePersistentData {}

trait BaseModel extends BasePersistentData {}
