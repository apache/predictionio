package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap

class FriendRecommendationTrainingData (
  // Designed to fit random and keyword similarity methods now
  // Will be updated to to fit more advanced algorithms when they are developed
  val userKeyword: HashMap[Int, HashMap[Int, Double]],
  val itemKeyword: HashMap[Int, HashMap[Int, Double]]
) extends Serializable
