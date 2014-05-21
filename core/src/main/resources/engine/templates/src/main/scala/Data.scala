package myengine

import io.prediction.{
  BaseEvaluationParams,
  BaseTrainingDataParams,
  BaseEvaluationDataParams,
  BaseTrainingData,
  BaseFeature,
  BaseTarget,
  BaseModel,
  BaseEvaluationUnit,
  BaseEvaluationResults
}

class EvaluationParams() extends BaseEvaluationParams {}

class TrainingDataParams() extends BaseTrainingDataParams {}

class EvaluationDataParams() extends BaseEvaluationDataParams {}

class TrainingData() extends BaseTrainingData {}

class Model() extends BaseModel {}

class Feature() extends BaseFeature {}

class Target() extends BaseTarget {}

class EvaluationUnit() extends BaseEvaluationUnit {}

class EvaluationResults extends BaseEvaluationResults {}
