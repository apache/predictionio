package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.collection.mutable.Stack

import scala.io.Source

// Note that this class is recommending user to user
// i.e. in the query the "item" should be a user id

case class SimRankModel (
  memoMatrix: Array[Array[Double]],
  userIdMap: Map[Int,Int]
) extends Serializable

/*
    This is the past query class
    Query and params
  */
case class SingleSimRankQuery (
  val userId: Int
) extends Serializable

/*
    Prediction Class
    This is the past prediction class, where query consists of one user id.
    The result given is a list of recommended item ids, sorted descending by
    simrank score.

case class Prediction (
  val recommendations: List[(Int,Double)]
) extends Serializable
*/

class SimRankAlgorithm (val ap: FriendRecommendationAlgoParams)
  extends LAlgorithm[FriendRecommendationAlgoParams, FriendRecommendationTrainingData,
    SimRankModel, FriendRecommendationQuery, FriendRecommendationPrediction] {

  override
  def train(td: FriendRecommendationTrainingData): SimRankModel = {
    /*
       Initialize identity matrix for base case of SimRank
    */
    val graphSize = td.socialAction.size
    var prevIter = Array.ofDim[Double](graphSize, graphSize)
    for(index <- 0 until graphSize)
      prevIter(index)(index) = 1

    // 6 iterations
    for(iter <- 0 until 6) {
      var currIter = Array.ofDim[Double](graphSize, graphSize)
      // Ignore nodes with no outgoing edges
      for(key <- 0 until graphSize) {
        if (td.socialAction(key) != null) {
          for((neighbor, weight) <- td.socialAction(key)) {
            val crossProduct =
              for(x<-td.socialAction(key); y<-td.socialAction(neighbor)) yield (x,y)
            val scalar = 0.8/(td.socialAction(key).map(tuple => tuple._2).sum * td.socialAction(neighbor).map(tuple => tuple._2).sum)
            for(pair <- crossProduct) {
              currIter(key)(neighbor) += scalar * prevIter(pair._1._1)(pair._2._1) * pair._1._2 * pair._2._2
            }
          }
        }
      }

      // Identity Constraint : Sim(a,a) = 1
      for(index <- 0 until graphSize)
        currIter(index)(index) = 1
      prevIter = currIter
    }
    SimRankModel(prevIter, td.userIdMap)
  }

  /*
  // Old prediction method
  override
  def predict(m: SimRankModel, query: SingleSimRankQuery): Prediction = {
    if(!m.matrixIdMap.contains(query.userId))
      Prediction(List())
    val reverseMap = m.matrixIdMap map {_.swap}
    val candidates = m.memoMatrix(m.matrixIdMap(query.userId))
                      .zipWithIndex
                      .sortWith((x:(Double,Int), y:(Double,Int)) => x._1 > y._1)
                      .toList
    val ordered = candidates.map((x:(Double,Int)) => (reverseMap(x._2), x._1))
    Prediction(ordered)
  }
  */

  override
  def predict(m: SimRankModel, query: FriendRecommendationQuery): FriendRecommendationPrediction = {
    if(m.userIdMap.contains(query.user) &&
       m.userIdMap.contains(query.item)) {
        val confidence = m.memoMatrix(m.userIdMap(query.user))(m.userIdMap(query.item))
        val acceptance = confidence > ap.threshold
        new FriendRecommendationPrediction(confidence, acceptance)
     } else {
        new FriendRecommendationPrediction(-1, false)
     }
   }
}

object SimRankEngineFactory extends IEngineFactory {
  override
  def apply() = {
    new Engine(
      classOf[FriendRecommendationDataSource],
      IdentityPreparator(classOf[FriendRecommendationDataSource]),
      Map("SimRankAlgorithm" -> classOf[SimRankAlgorithm]),
      FirstServing(classOf[SimRankAlgorithm]))
  }
}
