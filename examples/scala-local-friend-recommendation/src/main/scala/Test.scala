package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap

object Test {
  def main(args: Array[String]) {
    //val kwm1 = HashMap(1 -> 0.1, 2 -> 0.2, 3 -> 0.3)
    //val kwm2 = HashMap(3 -> 0.3, 4 -> 0.2, 5 -> 0.1)
    //val um = HashMap(1 -> kwm1, 2 -> kwm2)
    //val td = new KeywordSimilarityTrainingData(um, um)
    val ds = new FriendRecommendationDataSource()
    val td = ds.readTraining()
    val algo = new KeywordSimilarityAlgorithm(new FriendRecommendationAlgoParams(0.5))
    val model = algo.train(td)
    val query1 = new FriendRecommendationQuery(1, 1)
    val query2 = new FriendRecommendationQuery(2, 2)
    val query3 = new FriendRecommendationQuery(1, 2)
    val p1 = algo.predict(model, query1)
    println("(1, 1) => (" + p1.confidence + "," + p1.acceptance + ")")
    val p2 = algo.predict(model, query2)
    println("(2, 2) => (" + p2.confidence + "," + p2.acceptance + ")")
    val p3 = algo.predict(model, query3)
    println("(1, 2) => (" + p3.confidence + "," + p3.acceptance + ")")
  }
}
