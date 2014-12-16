package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.collection.immutable.HashMap
import scala.math

class KeywordSimilarityAlgorithm (val ap: FriendRecommendationAlgoParams)
  extends LAlgorithm[FriendRecommendationTrainingData,
    KeywordSimilarityModel, FriendRecommendationQuery, 
    FriendRecommendationPrediction] {

  override
  def train(td: FriendRecommendationTrainingData): KeywordSimilarityModel = {
    var keywordSimWeight = 0.0
    var keywordSimThreshold = 0.0
    new KeywordSimilarityModel(td.userIdMap, 
      td.itemIdMap, td.userKeyword, td.itemKeyword, 
      keywordSimWeight, keywordSimThreshold)
  }

  def findKeywordSimilarity(keywordMap1: HashMap[Int, Double], 
    keywordMap2: HashMap[Int, Double]): 
    Double = {
    var similarity = 0.0
    keywordMap1.foreach(kw => 
      similarity += kw._2 * keywordMap2.getOrElse(kw._1, 0.0))
    similarity
  }

  override
  def predict(model: KeywordSimilarityModel, 
    query: FriendRecommendationQuery): 
    FriendRecommendationPrediction = {
    // Currently use empty map for unseen users or items
    if (model.userIdMap.contains(query.user) && 
      model.itemIdMap.contains(query.item)) {
      val confidence = findKeywordSimilarity(
        model.userKeyword(model.userIdMap(query.user)),
        model.itemKeyword(model.itemIdMap(query.item)))
      val acceptance = confidence * 
        model.keywordSimWeight >= model.keywordSimThreshold
      new FriendRecommendationPrediction(confidence, acceptance)
    } else {
      val confidence = 0
      val acceptance = confidence * 
        model.keywordSimWeight >= model.keywordSimThreshold
      new FriendRecommendationPrediction(confidence, acceptance)
    }
  }
}
