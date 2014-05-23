package io.prediction

trait BaseEvaluationParams {}

trait BaseEvaluationUnit extends BasePersistentData {}

trait BaseEvaluationResults extends BasePersistentData {}


// Params
trait BaseCleanerParams {}

trait BaseAlgoParams {}

trait BaseServerParams {}

// Persistent Data

trait BasePersistentData {}

trait BaseTrainingData extends BasePersistentData {}

trait BaseCleansedData extends BasePersistentData {}

trait BaseFeature extends BasePersistentData {}

trait BasePrediction extends BasePersistentData {}

trait BaseActual extends BasePersistentData {}

trait BaseModel extends BasePersistentData {}


