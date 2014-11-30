package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap

object Test {
  def main(args: Array[String]) {
    val ds = new FriendRecommendationDataSource(
      new FriendRecommendationDataSourceParams(
        "data/item.txt", "data/user_key_word.txt", "data/user_action.txt", "data/rec_log_train.txt"))
    val td = ds.readTraining()
    val algo = new KeywordSimilarityAlgorithm(new FriendRecommendationAlgoParams(0.5))
    val model = algo.train(td)
    println(model.keywordSimWeight)
    println(model.keywordSimThreshold)
    val query1 = new FriendRecommendationQuery(2, 2)
    val query2 = new FriendRecommendationQuery(633246, 731434)
    val query3 = new FriendRecommendationQuery(1579051, 1774713)
    val p1 = algo.predict(model, query1)
    println("(1, 1) => (" + p1.confidence + "," + p1.acceptance + ")")
    val p2 = algo.predict(model, query2)
    println("(633246, 731434) => (" + p2.confidence + "," + p2.acceptance + ")")
    val p3 = algo.predict(model, query3)
    println("(1579051, 1774713) => (" + p3.confidence + "," + p3.acceptance + ")")
  }
}
