package io.prediction

trait BaseEvaluator[-EP, +TDP, +EDP, -F, -T] {
  def getParamsSet(params: EP): Seq[(TDP, EDP)]
  def evaluate(feature: F, predicted: T, actual: T): Unit
  def report(): Unit
}

trait BaseDataPreparator[-TDP, -EDP, +TD, +F, +T] {
  def prepareTraining(params: TDP): TD
  def prepareEvaluation(params: EDP): Seq[(F, T)]
}

trait BaseAlgorithm[-TD, +M] {
  def train(trainingData: TD): M
}

trait BaseServer[-M, -F, +T] {
  def predict(model: M, feature: F): T
}

