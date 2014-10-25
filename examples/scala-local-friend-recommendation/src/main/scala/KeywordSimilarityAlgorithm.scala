package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.collection.immutable.HashMap

class KeywordSimilarityAlgorithm extends LAlgorithm[EmptyAlgorithmParams, FriendRecommendationTrainingData,
  KeywordSimilarityModel, FriendRecommendationQuery, FriendRecommendationPrediction] {
  override
  def train(td: FriendRecommendationTrainingData): KeywordSimilarityModel = {
    new KeywordSimilarityModel(td.userKeyword, td.itemKeyword)
  }

  def findKeywordSimilarity(keywordMap1: HashMap[Int, Double], keywordMap2: HashMap[Int, Double]): Double = {
    var similarity = 0.0
    keywordMap1.foreach(kw => similarity += kw._2 * keywordMap2.getOrElse(kw._1, 0.0))
    similarity
  }

  override
  def predict(model: KeywordSimilarityModel, query: FriendRecommendationQuery): FriendRecommendationPrediction = {
    // Currently use empty map for unseen users or items
    val confidence = findKeywordSimilarity(model.userKeyword.getOrElse(query.user, HashMap[Int, Double]()),
                                           model.itemKeyword.getOrElse(query.item, HashMap[Int, Double]()))
    new FriendRecommendationPrediction(confidence)
  }
}
