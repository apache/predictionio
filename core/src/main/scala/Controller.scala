package io.prediction

// FIXME(yipjustin). I am lazy...
import io.prediction.core._

trait Cleanser[
    -TD <: BaseTrainingData,
    +CD <: BaseCleansedData,
    CP <: BaseCleanserParams]
  extends BaseCleanser[TD, CD, CP] {

  def init(params: CP): Unit

  def cleanse(trainingData: TD): CD
}

trait Algorithm[
    -CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction,
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
    extends Server[F, P, DefaultServerParams] {
  override def combine(feature: F, predictions: Seq[P]): P = predictions.head
}

class DefaultCleanser[TD <: BaseTrainingData]
    extends Cleanser[TD, TD, DefaultCleanserParams] {
  def init(params: DefaultCleanserParams): Unit = {}
  def cleanse(trainingData: TD): TD = trainingData
}

// Factory Methods
trait EngineFactory {
  def apply(): AbstractEngine
}
