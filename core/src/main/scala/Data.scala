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

// Persistent Data
trait BaseTrainingData extends BasePersistentData {}

//trait BaseCleansedData extends BasePersistentData {}
trait BaseCleansedData extends BaseTrainingData {}

trait BaseFeature extends BasePersistentData {}

trait BasePrediction extends BasePersistentData {}

trait BaseActual extends BasePersistentData {}

trait BaseModel extends BasePersistentData {}


