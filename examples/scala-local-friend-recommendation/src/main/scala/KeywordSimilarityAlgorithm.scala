package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.collection.immutable.HashMap

class KeywordSimilarityAlgorithm (val ap: FriendRecommendationAlgoParams)
extends LAlgorithm[FriendRecommendationAlgoParams, FriendRecommendationTrainingData,
  KeywordSimilarityModel, FriendRecommendationQuery, FriendRecommendationPrediction] {
  override
  def train(td: FriendRecommendationTrainingData): KeywordSimilarityModel = {
    new KeywordSimilarityModel(td.userIdMap, td.itemIdMap, td.userKeyword, td.itemKeyword)
  }

  def findKeywordSimilarity(keywordMap1: HashMap[Int, Double], keywordMap2: HashMap[Int, Double]): Double = {
    var similarity = 0.0
    keywordMap1.foreach(kw => similarity += kw._2 * keywordMap2.getOrElse(kw._1, 0.0))
    similarity
  }

  override
  def predict(model: KeywordSimilarityModel, query: FriendRecommendationQuery): FriendRecommendationPrediction = {
    // Currently use empty map for unseen users or items
    if (model.userIdMap.contains(query.user) && model.itemIdMap.contains(query.item)) {
      val confidence = findKeywordSimilarity(model.userKeyword(model.userIdMap(query.user)),
                                             model.itemKeyword(model.itemIdMap(query.item)))
      val acceptance = confidence > ap.threshold
      new FriendRecommendationPrediction(confidence, acceptance)
    } else {
      val confidence = 0
      val acceptance = confidence > ap.threshold
      new FriendRecommendationPrediction(confidence, acceptance)
    }
  }
}
