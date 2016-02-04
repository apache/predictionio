package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap

case class FriendRecommendationTrainingData (
  // Designed to fit random, keyword similarity methods and simrank now
  // Will be updated to to fit more advanced algorithms when they are developed
  // External-internal id map
  userIdMap: HashMap[Int, Int],
  itemIdMap: HashMap[Int, Int],
  // Keyword array, internal id index, term-weight map item
  userKeyword: Array[HashMap[Int, Double]],
  itemKeyword: Array[HashMap[Int, Double]],
  // User relationship array, 
  // src internal id index, dest-internal-id-weight list item
  socialAction: Array[List[(Int, Int)]],
  // Training record for training purpose
  trainingRecord: Stream[(Int, Int, Boolean)]
)
