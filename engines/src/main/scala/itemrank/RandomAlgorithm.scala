package io.prediction.engines.itemrank

import io.prediction.{ Algorithm }
import scala.util.Random

class RandomAlgorithm extends Algorithm[TrainingData, Feature, Prediction,
  RandomModel, RandomAlgoParams] {

  val rand = new Random(3) // TODO: pass seed from init()

  override def init(algoParams: RandomAlgoParams): Unit = {} // TODO

  override def train(trainingData: TrainingData): RandomModel = {
    new RandomModel()
  }

  override def predict(model: RandomModel, feature: Feature): Prediction = {
    val items = feature.items

    new Prediction (
      items = rand.shuffle(items).zip((items.size to 1 by -1).map(_.toDouble))
    )
  }

}
