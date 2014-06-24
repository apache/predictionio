package io.prediction
import scala.reflect.ClassTag

// FIXME(yipjustin). I am lazy...
import io.prediction.core._

trait Cleanser[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    CP <: BaseCleanserParams]
  extends LocalCleanser[TD, CD, CP] {

  //def init(params: CP): Unit

  def cleanse(trainingData: TD): CD
}

trait Algorithm[
    CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams]
    extends LocalAlgorithm[CD, F, P, M, AP] {
  //def init(algoParams: AP): Unit

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}

trait Server[-F <: BaseFeature, P <: BasePrediction, SP <: BaseServerParams]
    extends BaseServer[F, P, SP] {
  //def init(serverParams: SP): Unit

  def combine(feature: F, predictions: Seq[P]): P
}

// Below is default implementation.
class DefaultServer[F <: BaseFeature, P <: BasePrediction]
//    extends Server[F, P, DefaultServerParams] {
//  def init(params: DefaultServerParams): Unit = {}
    extends Server[F, P, EmptyParams] {
  //def init(params: EmptyParams): Unit = {}
  override def combine(feature: F, predictions: Seq[P]): P = predictions.head
}


class DefaultCleanser[TD <: BaseTrainingData : Manifest]()
    extends LocalCleanser[TD, TD, EmptyParams] {
  //def init(params: EmptyParams): Unit = {}
  def cleanse(trainingData: TD): TD = trainingData
}


class SparkDefaultCleanser[TD <: BaseTrainingData]()
    extends SparkCleanser[TD, TD, EmptyParams] {
  //def init(params: EmptyParams): Unit = {}
  def cleanse(trainingData: TD): TD = trainingData
}

/*
class DefaultCleanser[TD <: BaseTrainingData : Manifest]
    extends Cleanser[TD, TD, EmptyParams] {
  def init(params: EmptyParams): Unit = {}
  def cleanseBase(trainingData: BaseTrainingData)
  : BaseTrainingData = trainingData
}
*/


// Factory Methods
trait EngineFactory {
  def apply(): BaseEngine[
    _ <: BaseTrainingData,
    _ <: BaseCleansedData,
    _ <: BaseFeature,
    _ <: BasePrediction]
}
