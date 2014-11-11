package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.collection.mutable.Stack

import scala.io.Source

case class SimRankModel (
  memoMatrix: Array[Array[Double]],
  matrixIdMap: Map[Int,Int]
) extends Serializable

/*
    Training Data
  */
case class GraphData (
  adjList: Map[Int,List[(Int,(Int,Int))]],
  // Vertex count is a superset of adjacency list's keys because we have
  // digraph.
  vertices: Set[Int],
  matrixIdMap: Map[Int,Int]
) extends Serializable

/*
    Query and params
  */
case class SingleSimRankQuery (
  val userId: Int
) extends Serializable

case class PairwiseSimRankQuery (
  val user: Int,
  val item: Int
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

/*
  Pairwise Prediction Class
*/
case class Prediction (
  val confidence: Double,
  val acceptance: Boolean
) extends Serializable

class GraphDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams,
  GraphData, PairwiseSimRankQuery, EmptyActual] {

  /*
      Load Training Data
      Data representation is adjacency list, represented by map from user ID to
      a stack of (vertexID:Int, weight:Int).
      Stack chosen for O(1) insertion cost + iterator.
  */
  override
  def readTraining(): GraphData = {
    var uniqueIds = Set[Int]()
    val path = "To Data"
    val weightedEdges = Source.fromFile(path)
                      .getLines()
                      .map((x:String) => {
                            val splits = x.split('\t').map(s => s.toInt)
                            uniqueIds = uniqueIds + splits(0)
                            uniqueIds = uniqueIds + splits(1)
                            (splits(0),(splits(1), splits.slice(2,5).sum))
                          })
                      .toList

    // Aggregate (UID, (UID2,Weight)) tuples by UID
    val adjList = weightedEdges.groupBy(_._1)
    // println(lines)
    println(s"UNIQUE ID ${uniqueIds}")
    println(adjList)
    return GraphData(adjList, uniqueIds, uniqueIds.zipWithIndex.toMap)
  }

}

class SimRankAlgorithm (val ap: FriendRecommendationAlgoParams)
  extends LAlgorithm[EmptyAlgorithmParams, GraphData,
    SimRankModel, PairwiseSimRankQuery, Prediction] {

  override
  def train(gd: GraphData): SimRankModel = {
    /*
       Initialize identity matrix for base case of SimRank
    */
    var prevIter = Array.ofDim[Double](gd.vertices.size, gd.vertices.size)
    for(index <- 0 until gd.vertices.size)
      prevIter(index)(index) = 1

    for(iter <- 0 until 6) {
      var currIter = Array.ofDim[Double](gd.vertices.size, gd.vertices.size)
      // Prune nodes with no outgoing edges
      for(key <- gd.adjList.keys) {
        for((k, (neighbor, weight)) <- gd.adjList(key)) {
          if(gd.adjList.contains(neighbor)) {
            // Map vertex id to matrix id
            val keyId = gd.matrixIdMap(key)
            val neighborId = gd.matrixIdMap(neighbor)
            val crossProduct =
                      for(x<-gd.adjList(key); y<-gd.adjList(neighbor)) yield (x,y)
            val scalar = 0.8/(gd.adjList(key).size * gd.adjList(neighbor).size)
            for(pair <- crossProduct) {
              val pairId1 = gd.matrixIdMap(pair._1._2._1)
              val pairId2 = gd.matrixIdMap(pair._2._2._1)
              currIter(keyId)(neighborId) += scalar * prevIter(pairId1)(pairId2)
            }
          }
        }
      }

      // Identity Constraint : Sim(a,a) = 1
      for(index <- 0 until gd.vertices.size)
        currIter(index)(index) = 1
      prevIter = currIter
    }
    SimRankModel(prevIter, gd.matrixIdMap)
  }

  /*
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
}
  */

  override
  def predict(m: SimRankModel, query: PairwiseSimRankQuery): Prediction = {
    if(m.matrixIdMap.contains(query.user) &&
       m.matrixIdMap.contains(query.item)) {
        val confidence = m.memoMatrix(m.matrixIdMap(query.user))(m.matrixIdMap(query.item))
        val acceptance = confidence > ap.threshold
        Prediction(confidence, acceptance)
     } else {
        Prediction(-1, false)
     }
   }
}

object SimRankEngineFactory extends IEngineFactory {
  override
  def apply() = {
    new Engine(
      classOf[GraphDataSource],
      IdentityPreparator(classOf[GraphDataSource]),
      Map("SimRankAlgorithm" -> classOf[SimRankAlgorithm]),
      FirstServing(classOf[SimRankAlgorithm]))
  }
}
