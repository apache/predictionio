package io.prediction.examples.friendrecommendation
import io.prediction.controller._
import scala.io.Source
import collection.mutable.HashMap

class FriendRecommendationDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams, FriendRecommendationTrainingData, FriendRecommendationQuery, EmptyActual] {
  override
  def readTraining() : FriendRecommendationTrainingData = {
    val item = Source.fromFile("data/item.txt").getLines().map{ line =>
      val data = line.split("\\s+")
      val id = data(1).toInt
      //key is value (Int) and value is its weight(Double)
      var item_map = new HashMap[Int, Double]
      for (keyword <- data(3).split(";")){
        //Item keyword contain no weight
        item_map += (keyword.toInt -> 1.0)
      }
      (id, item_map)
    }
    var item_keyword = new HashMap[Int, HashMap[Int, Double]]
    for(element <- item){  
      item_keyword += (element._1 -> element._2)
    }

    //Reading user profile
    val user = Source.fromFile("data/user_profile.txt").getLines().map{ line =>
      val data = line.split("\\s+")
      val id = data(1).toInt
      //key is value (Int) and value is its weight(Double)
      var user_map = new HashMap[Int, Double]
      for (keyword <- data(5).split(";")){
        //Item keyword contain no weight
        user_map += (keyword.toInt -> 1.0)
      }
      (id, user_map)
    }

    var user_keyword = new HashMap[Int, HashMap[Int, Double]]
    for(element <- user){
      user_keyword += (element._1 -> element._2)
       
    }
    //val user_kw_map = collection.immutable.HashMap() ++ user_keyword
    //val item_kw_map = collection.immutable.HashMap() ++ item_keyword
    new FriendRecommendationTrainingData(user_keyword, item_keyword)

  }
}
