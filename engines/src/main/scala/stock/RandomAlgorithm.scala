package io.prediction.engines.stock

import io.prediction.controller.Params
import io.prediction.controller.LAlgorithm
import scala.util.Random

case class RandomAlgorithmParams(
  val seed: Long = 709394,
  val scale: Double = 0.01,
  val drift: Double = 0.0) extends Params {}


class RandomAlgorithm(val params: RandomAlgorithmParams)
    extends LAlgorithm[RandomAlgorithmParams, TrainingData, Unit, 
        Feature, Target] {
  @transient lazy val _random: Random = new Random(params.seed)

  def train(trainingData: TrainingData): Unit = {}

  def predict(model: Unit, feature: Feature): Target = {
    val tickers = feature.data.colIx.toVec.contents
    val prediction = tickers.map {
      ticker => (ticker, params.drift + _random.nextGaussian() * params.scale)
    }.toMap
    new Target(feature.tomorrow, data = prediction)
  }

  override def toString() = "RandomAlgorithm"
}
