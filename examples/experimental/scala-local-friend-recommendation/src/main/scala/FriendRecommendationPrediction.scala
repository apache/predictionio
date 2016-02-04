package io.prediction.examples.friendrecommendation

case class FriendRecommendationPrediction (
  confidence: Double,
  // returning boolean acceptance to align with KDD 2012 scenario
  acceptance: Boolean
)
