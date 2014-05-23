package io.prediction

trait BaseEvaluationParams {}

trait BaseEvaluationUnit extends BasePersistentData {}

trait BaseEvaluationResults extends BasePersistentData {}



//trait BaseTrainingDataParams {}

//trait BaseEvaluationDataParams {}

trait BasePersistentData {}

trait BaseTrainingData extends BasePersistentData {}

trait BaseProcessedData extends BasePersistentData {}

trait BaseFeature extends BasePersistentData {}

trait BasePrediction extends BasePersistentData {}

trait BaseActual extends BasePersistentData {}

trait BaseModel extends BasePersistentData {}

trait BaseAlgoParams {}

trait BaseServerParams {}

