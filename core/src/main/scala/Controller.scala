package io.prediction

//import io.prediction.BaseParams

// FIXME(yipjustin). I am lazy...
import io.prediction.core._

trait Cleanser[TD, CD, CP <: BaseParams]
  extends LocalCleanser[TD, CD, CP] {
  def cleanse(trainingData: TD): CD
}

trait Algorithm[CD, F, P, M, AP <: BaseParams]
  extends LocalAlgorithm[CD, F, P, M, AP] {
  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}

trait Server[F, P, SP <: BaseParams]
  extends BaseServer[F, P, SP] {
  def combine(feature: F, predictions: Seq[P]): P
}

// Below is default implementation.
class DefaultServer[F, P] extends Server[F, P, Null] {
  override def combine(feature: F, predictions: Seq[P]): P = predictions.head
}

class DefaultCleanser[TD : Manifest]()
  extends LocalCleanser[TD, TD, Null] {
  def cleanse(trainingData: TD): TD = trainingData
}

class SparkDefaultCleanser[TD]()
    extends SparkCleanser[TD, TD, Null] {
  def cleanse(trainingData: TD): TD = trainingData
}

/*
class BaseDefaultCleanser[TD]()
  extends BaseCleanser[TD, TD, EmptyParams] {
  def cleanseBase(trainingData: Any): TD = {
    trainingData.asInstanceOf[TD]
  }
}
*/

// Factory Methods
trait EngineFactory {
  def apply(): BaseEngine[_,_,_,_]
}
