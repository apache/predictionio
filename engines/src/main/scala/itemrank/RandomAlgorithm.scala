package io.prediction.engines.itemrank

import io.prediction.controller.Params
import io.prediction.controller.LAlgorithm

import scala.util.Random

class RandomAlgoParams() extends Params {
  override def toString = s"empty"
}

class RandomModel() extends Serializable {}

class RandomAlgorithm(params: RandomAlgoParams)
  extends LAlgorithm[RandomAlgoParams, PreparedData, RandomModel,
  Query, Prediction] {

  @transient lazy val rand = new Random(3) // TODO: pass seed from init()

  override def train(preparedData: PreparedData): RandomModel = {
    new RandomModel()
  }

  override def predict(model: RandomModel, query: Query): Prediction = {
    val items = query.iids

    new Prediction (
      items = rand.shuffle(items).zip((items.size to 1 by -1).map(_.toDouble))
    )
  }

}
