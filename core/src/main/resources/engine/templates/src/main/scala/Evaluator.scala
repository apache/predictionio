package myengine

import io.prediction.Evaluator

class MyEvaluator extends Evaluator[
    EvaluationParams,
    TrainingDataParams,
    EvaluationDataParams,
    Feature,
    Prediction,
    Actual,
    EvaluationUnit,
    EvaluationResults] {

  def getParamsSet(params: EvaluationParams):
    Seq[(TrainingDataParams, EvaluationDataParams)] = {
    Seq[(TrainingDataParams, EvaluationDataParams)]()
  }

  def evaluate(feature: Feature, predicted: Prediction, actual: Actual):
    EvaluationUnit = {
    new EvaluationUnit()
  }

  def report(evalUnits: Seq[EvaluationUnit]): EvaluationResults = {
    new EvaluationResults()
  }

}
