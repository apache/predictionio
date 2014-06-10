package io.prediction.engines.stock

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
  var _seed: Long = 0
  // Notice that parallization may mess-up reproduceability of a fixed seed.
  @transient lazy val _random: Random = new Random(_seed)
  override def init(algoParams: RandomAlgoParams): Unit = {
    println("RandomAlgorithm.init")
    //_random = new Random(algoParams.seed)
    _seed = algoParams.seed
    _scale = algoParams.scale
    _drift = algoParams.drift
  }

  def train(trainingData: TrainingData) = {
    println("RandomAlgorithm.train")
    new EmptyModel()
  }

  def predict(model: EmptyModel, feature: Feature): Target = {
    println(s"RandomAlgorithm.predict ${_seed} ${_scale} ${_drift}")
    val tickers = feature.data.colIx.toVec.contents
    //val tickers = feature.boxedData.get.colIx.toVec.contents
    val prediction = tickers.map {
      ticker => (ticker, _drift + _random.nextGaussian() * _scale)
    }.toMap
    new Target(data = prediction)
  }

  override def toString() = "RandomAlgorithm"
}
