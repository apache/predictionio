package io.prediction.examples.friendrecommendation

import io.prediction.controller._

object RandomEngineFactory extends IEngineFactory {
  override
  def apply() = {
    new Engine(
      classOf[FriendRecommendationDataSource],
      IdentityPreparator(classOf[FriendRecommendationDataSource]),
      Map("RandomAlgorithm" -> classOf[RandomAlgorithm]),
      FirstServing(classOf[RandomAlgorithm])
    )
  }
}
