package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.io.Source
import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

class FriendRecommendationDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams, FriendRecommendationTrainingData, FriendRecommendationQuery, EmptyActual] {
  override
  def readTraining() : FriendRecommendationTrainingData = {
    val (itemIdMap, itemKeyword) = readItem("data/mini_item.txt")
    val (userIdMap, userKeyword) = readUser("data/mini_user_key_word.txt")
    val adjArray = readRelationship("data/mini_user_action.txt", userKeyword.size, userIdMap)
    new FriendRecommendationTrainingData(userIdMap, itemIdMap, userKeyword, itemKeyword, adjArray)
  }

  def readItem(file: String) : (HashMap[Int, Int], Array[HashMap[Int, Double]]) = {
    val lines = Source.fromFile(file).getLines()
    // An array on Map[keywordId -> weight] values with internal item id index
    val itemKeyword = new ArrayBuffer[HashMap[Int, Double]]()
    // A map from external id to internal id
    var itemIdMap = new HashMap[Int, Int]()
    var internalId = 0
    lines.foreach{
      line =>
      val data = line.split("\\s")
      itemIdMap += (data(0).toInt -> internalId)
      val keywordMap = HashMap(
        (for (term <- data(2).split(";")) yield (term.toInt -> 1.0))
        .toSeq
      : _*)
      itemKeyword.append(keywordMap)
      internalId += 1
    }
    (itemIdMap, itemKeyword.toArray)
  }

  def readUser(file: String) : (HashMap[Int, Int], Array[HashMap[Int, Double]]) = {
    // Helper function to generate pair items form a:b strings
    val genTermWeightItem = (termWeight: String) => {
      val termWeightPair = termWeight.split(":")
      (termWeightPair(0).toInt -> termWeightPair(1).toDouble)
    }
    val lines = Source.fromFile(file).getLines()
    // An array on Map[keywordId -> weight] values with internal item id index
    val userKeyword = new ArrayBuffer[HashMap[Int, Double]]()
    // A map from external id to internal id
    var userIdMap = new HashMap[Int, Int]()
    var internalId = 0
    lines.foreach{
      line =>
      val data = line.split("\\s")
      userIdMap += (data(0).toInt -> internalId)
      val keywordMap = HashMap(
        (for (termWeight <- data(1).split(";")) yield
          genTermWeightItem(termWeight)
        ).toSeq
      : _*)
      userKeyword.append(keywordMap)
      internalId += 1
    }
    (userIdMap, userKeyword.toArray)
  }

  def readRelationship(file: String, userSize: Int, userIdMap: HashMap[Int, Int]) : Array[List[(Int, Int)]] = {
    val adjArray = new Array[List[(Int, Int)]](userSize)
    val lines = Source.fromFile(file).getLines()
    lines.foreach{
      line => 
      val data = line.split("\\s").map(s => s.toInt)
      if (userIdMap.contains(data(0)) && userIdMap.contains(data(1))) {
        val srcInternalId = userIdMap(data(0))
        val destInternalId = userIdMap(data(1))
        if (adjArray(srcInternalId) == null) {
          adjArray(srcInternalId) = (destInternalId, data.slice(2,5).sum)::List()
        } else {
          adjArray(srcInternalId) = (destInternalId, data.slice(2,5).sum)::adjArray(srcInternalId)
        }
      }
    }
    adjArray
  }
}
