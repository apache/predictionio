package io.prediction.stock

import io.prediction.{ BaseAlgorithm, BaseServer, BaseModel }

import scala.util.Random

class EmptyModel (
) extends BaseModel {}

class RandomAlgorithm extends BaseAlgorithm[TrainingData, EmptyModel] {
  def train(trainingData: TrainingData) = new EmptyModel()
}

class RandomServer extends BaseServer[EmptyModel, Feature, Target] {
  def predict(model: EmptyModel, feature: Feature): Target = {
    val tickers = feature.data.colIx.toVec.contents
    val prediction = tickers.map { 
      ticker => (ticker, Random.nextGaussian() / 100)
    }.toMap
    new Target(data = prediction)
  }
}
