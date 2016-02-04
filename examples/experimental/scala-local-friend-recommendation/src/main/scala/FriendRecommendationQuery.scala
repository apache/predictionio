package io.prediction.examples.friendrecommendation

case class FriendRecommendationQuery (
  // To align with the KDD 2012 scenario
  // Given a user and an item, predict acceptance
  user: Int,
  item: Int
)
