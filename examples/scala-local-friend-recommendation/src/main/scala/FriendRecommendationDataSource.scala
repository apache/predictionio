package io.prediction.examples.friendrecommendation

import io.prediction.controller._
import scala.io.Source
import scala.collection.immutable.HashMap

class FriendRecommendationDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams, FriendRecommendationTrainingData, FriendRecommendationQuery, EmptyActual] {
  override
  def readTraining() : FriendRecommendationTrainingData = {
    val itemKeywordMap = readItem("data/mini_item.txt")
    val userKeywordMap = readUser("data/mini_user_key_word.txt")
    new FriendRecommendationTrainingData(userKeywordMap, itemKeywordMap)
  }

  def readItem(file: String) : HashMap[Int, HashMap[Int, Double]] = {
    // An iterator on [itemId -> Map[keywordId -> weight]] values
    val itemKeywordIter = Source.fromFile(file).getLines().map{
      line =>
      val data = line.split("\\s")
      val keywordMap = HashMap[Int, Double](
        (for (term <- data(2).split(";")) yield (term.toInt -> 1.0))
        .toSeq
      : _*)
      (data(0).toInt -> keywordMap)
    }
    val itemKeywordMap = HashMap[Int, HashMap[Int, Double]](itemKeywordIter.toSeq : _*)
    itemKeywordMap
  }

  def readUser(file: String) : HashMap[Int, HashMap[Int, Double]] = {
    // Helper function to generate pair items form a:b strings
    val genTermWeightItem = (termWeight: String) => {
      val termWeightPair = termWeight.split(":")
      (termWeightPair(0).toInt -> termWeightPair(1).toDouble)
    }
    // An iterator on [userId -> Map[keywordId -> weight]] values
    val userKeywordIter = Source.fromFile(file).getLines().map{
      line =>
      val data = line.split("\\s")
      val keywordMap = HashMap[Int, Double](
        (for (termWeight <- data(1).split(";")) yield 
          genTermWeightItem(termWeight)
        ).toSeq
      : _*)
      (data(0).toInt -> keywordMap)
    }
    val userKeywordMap = HashMap(userKeywordIter.toSeq : _*)
    userKeywordMap
  }
}
