package io.prediction

trait Evaluator[-EP, +TDP <: BaseTrainingDataParams, 
    +EDP <: BaseEvaluationDataParams, -F, -T,
    EU <: BaseEvaluationUnit, ER <: BaseEvaluationResults]
    extends BaseEvaluator[EP, TDP, EDP, F, T, EU, ER] {
  def getParamsSet(params: EP): Seq[(TDP, EDP)]

  def evaluate(feature: F, predicted: T, actual: T): EU

  def report(evalUnits: Seq[EU]): ER
}

trait DataPreparator[-TDP, +TD <: BaseTrainingData]
    extends BaseDataPreparator[TDP, TD] {
  def prepareTraining(params: TDP): TD
}

trait EvaluationPreparator[-EDP, +F <: BaseFeature, +T <: BaseTarget]
    extends BaseEvaluationPreparator[EDP, F, T] {
  def prepareEvaluation(params: EDP): Seq[(F, T)]
}

trait Algorithm[-TD, -F, +T <: BaseTarget, M <: BaseModel, 
    AP <: BaseAlgoParams]
    extends BaseAlgorithm[TD, F, T, M, AP] {
  def init(algoParams: AP): Unit

  def train(trainingData: TD): M

  def predict(model: M, feature: F): T
}

trait Server[-F, T <: BaseTarget, SP <: BaseServerParams] 
    extends BaseServer[F, T, SP] {
  def init(serverParams: SP): Unit

  def combine(feature: F, targets: Seq[T]): T
}

// Below is default implementation.
class DefaultServer[F, T <: BaseTarget] extends Server[F, T, BaseServerParams] {
  override def combine(feature: F, targets: Seq[T]): T = targets.head
}
