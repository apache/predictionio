package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap

class FriendRecommendationModel (
  /*
   * Mapping a user to a map from item to confidence
   * This design is for adapting to future common friend recommendation case where we give top items for a user
   * instead of give the confidence of a user-item pair in the competition
   * In that case, change the inner Map to List should be OK
   */
  // val itemConfidenceMap: HashMap[Int, HashMap[Int, Double]]
  /*
   *  This design is to fit the competition
   */
  val itemConfidenceMap: HashMap[(Int, Int), Double]
) extends Serializable
