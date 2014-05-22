package io.prediction


trait BaseEvaluationParams {}

trait BaseTrainingDataParams {}

trait BaseEvaluationDataParams {}

trait BasePersistentData {}

trait BaseTrainingData extends BasePersistentData {}

trait BaseFeature extends BasePersistentData {}

trait BasePrediction extends BasePersistentData {}

trait BaseActual extends BasePersistentData {}

trait BaseModel extends BasePersistentData {}

trait BaseAlgoParams {}

trait BaseServerParams {}

trait BaseEvaluationUnit extends BasePersistentData {}

trait BaseEvaluationResults extends BasePersistentData {}
