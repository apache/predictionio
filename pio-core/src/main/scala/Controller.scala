package io.prediction


trait Evaluator[-EP, +TDP <: BaseTrainingDataParams,
  +EDP <: BaseEvaluationDataParams, -F, -T] 
extends BaseEvaluator[EP, TDP, EDP, F, T] {
  def getParamsSet(params: EP): Seq[(TDP, EDP)]

  def evaluate(feature: F, predicted: T, actual: T): Unit

  def report(): Unit
}

trait DataPreparator[-TDP, +TD <: BaseTrainingData]
extends BaseDataPreparator[TDP, TD] {
  def prepareTraining(params: TDP): TD
}

trait EvaluationPreparator[-EDP, +F <: BaseFeature, +T <: BaseTarget]
extends BaseEvaluationPreparator[EDP, F, T]{
  def prepareEvaluation(params: EDP): Seq[(F, T)]
}

trait Algorithm[-TD, -F, +T <: BaseTarget, 
  M <: BaseModel, AP <: BaseAlgoParams]
extends BaseAlgorithm[TD, F, T, M, AP] {
  def init(algoParams: AP): Unit

  def train(trainingData: TD): M

  def predict(model: M, feature: F): T
}

trait Server[-F, T <: BaseTarget] extends BaseServer[F, T] {
  def combine(feature: F, targets: Seq[T]): T
}

// Below is default implementation.
class DefaultServer[F, T <: BaseTarget] extends Server[F, T] {
  override def combine(feature: F, targets: Seq[T]): T = targets.head
}
