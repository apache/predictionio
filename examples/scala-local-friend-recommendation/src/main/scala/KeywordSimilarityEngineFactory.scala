package io.prediction.examples.friendrecommendation

import io.prediction.controller._

object KeywordSimilarityEngineFactory extends IEngineFactory {
  override
  def apply() = {
    new Engine(
      classOf[FriendRecommendationDataSource],
      IdentityPreparator(classOf[FriendRecommendationDataSource]),
      Map("KeywordSimilarityAlgorithm" -> classOf[KeywordSimilarityAlgorithm]),
      FirstServing(classOf[KeywordSimilarityAlgorithm]))
  }
}
