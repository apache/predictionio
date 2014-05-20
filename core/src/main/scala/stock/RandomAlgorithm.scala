package io.prediction.stock

import io.prediction.{ Algorithm, BaseModel, BaseAlgoParams }

import scala.util.Random

class EmptyModel() extends BaseModel {}

class RandomAlgoParams(
  val seed: Long = 709394,
  val scale: Double = 0.01,
  val drift: Double = 0.0) extends BaseAlgoParams {}

class RandomAlgorithm
    extends Algorithm[TrainingData, Feature, Target, EmptyModel, RandomAlgoParams] {
  var _scale: Double = 0.0
  var _drift: Double = 0.0
  // Notice that parallization may mess-up reproduceability of a fixed seed.
  var _random: Random = null
  override def init(algoParams: RandomAlgoParams): Unit = {
    _random = new Random(algoParams.seed)
    _scale = algoParams.scale
    _drift = algoParams.drift
  }

  def train(trainingData: TrainingData) = new EmptyModel()
  def predict(model: EmptyModel, feature: Feature): Target = {
    val tickers = feature.data.colIx.toVec.contents
    val prediction = tickers.map {
      ticker => (ticker, _drift + _random.nextGaussian() * _scale)
    }.toMap
    new Target(data = prediction)
  }
}
