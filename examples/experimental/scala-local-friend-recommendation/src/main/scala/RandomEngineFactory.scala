package io.prediction.examples.friendrecommendation

import io.prediction.controller._

object RandomEngineFactory extends IEngineFactory {
  override
  def apply() = {
    new Engine(
      classOf[FriendRecommendationDataSource],
      classOf[LIdentityPreparator[FriendRecommendationTrainingData]],
      Map("RandomAlgorithm" -> classOf[RandomAlgorithm]),
      classOf[LFirstServing[FriendRecommendationQuery, FriendRecommendationPrediction]])
  }
}
