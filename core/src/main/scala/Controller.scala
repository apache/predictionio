package io.prediction

// FIXME(yipjustin). I am being lazy..
import io.prediction.core._

trait Evaluator[
    EP <: BaseEvaluationParams,
    TDP <: BaseTrainingDataParams,
    EDP <: BaseEvaluationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    EU <: BaseEvaluationUnit,
    ER <: BaseEvaluationResults]
    extends BaseEvaluator[EP, TDP, EDP, TD, F, P, A, EU, ER] {

  // Data generation
  def getParamsSet(params: EP): Seq[(TDP, EDP)]
  
  def prepareTraining(params: TDP): TD
  
  def prepareEvaluation(params: EDP): Seq[(F, A)]
 
  // Evaluation
  def init(params: EP): Unit

  def evaluate(feature: F, predicted: P, actual: A): EU

  def report(evalUnits: Seq[EU]): ER
}
    

trait Algorithm[
    -CD <: BaseCleansedData,
    -F <: BaseFeature,
    +P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams]
    extends BaseAlgorithm[CD, F, P, M, AP] {
  def init(algoParams: AP): Unit

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}

trait Server[-F <: BaseFeature, P <: BasePrediction, SP <: BaseServerParams]
    extends BaseServer[F, P, SP] {
  def init(serverParams: SP): Unit

  def combine(feature: F, predictions: Seq[P]): P
}

// Below is default implementation.
class DefaultServer[F <: BaseFeature, P <: BasePrediction]
    extends Server[F, P, BaseServerParams] {
  override def combine(feature: F, predictions: Seq[P]): P = predictions.head
}

class DefaultCleanser[CD <: BaseCleansedData]
    extends BaseCleanser[CD, CD, BaseCleanserParams] {
  def init(params: BaseCleanserParams): Unit = {}
  def cleanse(trainingData: CD): CD = trainingData
}
