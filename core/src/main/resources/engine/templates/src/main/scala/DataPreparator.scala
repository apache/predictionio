package myengine

import io.prediction.DataPreparator
import io.prediction.EvaluationPreparator

class MyPreparator
    extends DataPreparator[TrainingDataParams, TrainingData]
    with EvaluationPreparator[EvaluationDataParams, Feature, Target] {

  def prepareTraining(params: TrainingDataParams): TrainingData = {
    new TrainingData()
  }

  def prepareEvaluation(params: EvaluationDataParams): Seq[(Feature, Target)] = {
    Seq[(Feature, Target)]()
  }
}
