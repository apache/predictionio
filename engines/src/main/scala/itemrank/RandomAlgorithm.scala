package io.prediction.engines.itemrank

import io.prediction.controller.LAlgorithm
import scala.util.Random

class RandomAlgorithm(params: RandomAlgoParams)
  extends LAlgorithm[RandomAlgoParams, PreparedData, RandomModel,
  Query, Prediction] {

  @transient lazy val rand = new Random(3) // TODO: pass seed from init()

  override def train(preparedData: PreparedData): RandomModel = {
    new RandomModel()
  }

  override def predict(model: RandomModel, query: Query): Prediction = {
    val items = query.items

    new Prediction (
      items = rand.shuffle(items).zip((items.size to 1 by -1).map(_.toDouble))
    )
  }

}
