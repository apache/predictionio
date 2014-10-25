package io.prediction.examples.friendrecommendation

import io.prediction.controller._

// For random algorithm
import scala.util.Random

class RandomAlgorithm extends LAlgorithm[EmptyAlgorithmParams, FriendRecommendationTrainingData,
  RandomModel, FriendRecommendationQuery, FriendRecommendationPrediction] {
  override
  def train(pd: FriendRecommendationTrainingData): RandomModel = {
    new RandomModel() 
  }

  override
  def predict(model: RandomModel, query: FriendRecommendationQuery): FriendRecommendationPrediction = {
    val randomConfidence = Random.nextDouble
    new FriendRecommendationPrediction(randomConfidence)
  }
}
