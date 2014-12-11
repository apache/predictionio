package io.prediction.examples.friendrecommendation

import io.prediction.controller._

object KeywordSimilarityEngineFactory extends IEngineFactory {
  override
  def apply() = {
    new Engine(
      classOf[FriendRecommendationDataSource],
      classOf[LIdentityPreparator[FriendRecommendationTrainingData]],
      Map("KeywordSimilarityAlgorithm" -> classOf[KeywordSimilarityAlgorithm]),
      classOf[LFirstServing[FriendRecommendationQuery, FriendRecommendationPrediction]])
  }
}
