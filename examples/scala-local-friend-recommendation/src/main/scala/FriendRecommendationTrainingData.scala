package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap
//import scala.collection.mutable.HashMap

class FriendRecommendationTrainingData (
  // Designed to fit random and keyword similarity methods now
  // Will be updated to to fit more advanced algorithms when they are developed
  
  //List of userid and itemid
  //val userId: List[Int],
  //val itemId: List[Int]
  
  val userKeyword: HashMap[Int, HashMap[Int, Double]],
  val itemKeyword: HashMap[Int, HashMap[Int, Double]]
) extends Serializable
