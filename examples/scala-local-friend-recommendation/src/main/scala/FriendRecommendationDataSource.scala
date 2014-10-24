package io.prediction.examples.friendrecommendation
import io.prediction.controller._
import scala.io.Source

class FriendRecommendationDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams, FriendRecommendationTrainingData, FriendRecommendationQuery, EmptyActual] {
  override
  def readTraining() : FriendRecommendationTrainingData = {
    val item = Source.fromFile("data/item.txt").getLines().map{ line =>
      val data = line.split("\\s+")
      data(1).toInt
    }.toList
    val user = Source.fromFile("data/user_profile.txt").getLines().map{ line =>
      val data = line.split("\\s+")
      data(1).toInt
    }.toList
    new FriendRecommendationTrainingData(user, item)

  }
}
