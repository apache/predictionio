package io.prediction

trait Evaluator[
    -EP <: BaseEvaluationParams,
    +TDP <: BaseTrainingDataParams,
    +EDP <: BaseEvaluationDataParams,
    -F <: BaseFeature,
    -P <: BasePrediction,
    -A <: BaseActual,
    EU <: BaseEvaluationUnit,
    ER <: BaseEvaluationResults]
    extends BaseEvaluator[EP, TDP, EDP, F, P, A, EU, ER] {

  def init(params: EP): Unit
  
  def getParamsSet(params: EP): Seq[(TDP, EDP)]

  def evaluate(feature: F, predicted: P, actual: A): EU

  def report(evalUnits: Seq[EU]): ER
}

trait DataPreparator[-TDP, +TD <: BaseTrainingData]
    extends BaseDataPreparator[TDP, TD] {
  def prepareTraining(params: TDP): TD
}

trait EvaluationPreparator[-EDP, +F <: BaseFeature, +A <: BaseActual]
    extends BaseEvaluationPreparator[EDP, F, A] {
  def prepareEvaluation(params: EDP): Seq[(F, A)]
}

trait Algorithm[
    -TD <: BaseTrainingData,
    -F <: BaseFeature,
    +P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams]
    extends BaseAlgorithm[TD, F, P, M, AP] {
  def init(algoParams: AP): Unit

  def train(trainingData: TD): M

  def predict(model: M, feature: F): P
}

trait Server[-F, P <: BasePrediction, SP <: BaseServerParams]
    extends BaseServer[F, P, SP] {
  def init(serverParams: SP): Unit

  def combine(feature: F, predictions: Seq[P]): P
}

// Below is default implementation.
class DefaultServer[F, P <: BasePrediction]
    extends Server[F, P, BaseServerParams] {
  override def combine(feature: F, predictions: Seq[P]): P = predictions.head
}
