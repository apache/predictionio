package io.prediction.examples.friendrecommendation

import io.prediction.controller._

object KeywordSimilarityEngineFactory extends IEngineFactory {
  override
  def apply() = {
    new SimpleEngine(
      classOf[FriendRecommendationDataSource],
      classOf[KeywordSimilarityAlgorithm]
    )
  }
}
