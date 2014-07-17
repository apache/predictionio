package io.prediction.engines.stock

import io.prediction.api.Params
import io.prediction.api.LAlgorithm
import scala.util.Random

case class RandomAlgorithmParams(
  val seed: Long = 709394,
  val scale: Double = 0.01,
  val drift: Double = 0.0) extends Params {}


class RandomAlgorithm2(val params: RandomAlgorithmParams)
    extends LAlgorithm[RandomAlgorithmParams, TrainingData, Unit, 
        Feature, Target2] {
  @transient lazy val _random: Random = new Random(params.seed)

  def train(trainingData: TrainingData): Unit = {}

  def predict(model: Unit, feature: Feature): Target2 = {
    val tickers = feature.data.colIx.toVec.contents
    val prediction = tickers.map {
      ticker => (ticker, params.drift + _random.nextGaussian() * params.scale)
    }.toMap
    new Target2(feature.tomorrow, data = prediction)
  }

  override def toString() = "RandomAlgorithm"
}




/**** OLD ****/
import io.prediction.{ Algorithm, BaseParams }

import scala.util.Random


class RandomAlgoParams(
  val seed: Long = 709394,
  val scale: Double = 0.01,
  val drift: Double = 0.0) extends BaseParams {}


class RandomAlgorithm
    extends Algorithm[TrainingData, Feature, Target, Unit, RandomAlgoParams] {
  var _scale: Double = 0.0
  var _drift: Double = 0.0
  var _seed: Long = 0
  // Notice that parallization may mess-up reproduceability of a fixed seed.
  @transient lazy val _random: Random = new Random(_seed)
  override def init(algoParams: RandomAlgoParams): Unit = {
    println("RandomAlgorithm.init")
    _seed = algoParams.seed
    _scale = algoParams.scale
    _drift = algoParams.drift
  }

  def train(trainingData: TrainingData): Unit = {
    println("RandomAlgorithm.train")
  }

  def predict(model: Unit, feature: Feature): Target = {
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
