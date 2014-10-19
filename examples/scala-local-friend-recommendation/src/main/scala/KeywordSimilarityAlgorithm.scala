package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.collection.immutable.HashMap

class KeywordSimilarityAlgorithm extends LAlgorithm[EmptyAlgorithmParams, KeywordSimilarityTrainingData,
  FriendRecommendationModel, FriendRecommendationQuery, FriendRecommendationPrediction] {
  override
  def train(td: KeywordSimilarityTrainingData): FriendRecommendationModel = {
    val model = HashMap(
                  (for (user <- td.userKeyword.keys; item <- td.itemKeyword.keys)
                    yield (user, item) -> 
                      findKeywordSimilarity(td.userKeyword(user), td.itemKeyword(item))
                  ).toSeq
                : _*)
    new FriendRecommendationModel(model)
  }

  def findKeywordSimilarity(keywordMap1: HashMap[Int, Double], keywordMap2: HashMap[Int, Double]): Double = {
    var similarity = 0.0
    keywordMap1.foreach(kw => similarity += kw._2 * keywordMap2.getOrElse(kw._1, 0.0))
    similarity
  }

  override
  def predict(model: FriendRecommendationModel, query: FriendRecommendationQuery): FriendRecommendationPrediction = {
    val confidence = model.itemConfidenceMap.getOrElse((query.user, query.item), 0.0)
    new FriendRecommendationPrediction(confidence)
  }
}
