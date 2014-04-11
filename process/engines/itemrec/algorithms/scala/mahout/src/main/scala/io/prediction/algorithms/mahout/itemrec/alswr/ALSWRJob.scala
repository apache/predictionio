package io.prediction.algorithms.mahout.itemrec.alswr

import scala.collection.JavaConversions._

import io.prediction.algorithms.mahout.itemrec.MahoutJob
import io.prediction.algorithms.mahout.itemrec.AllPreferredItemsNeighborhoodCandidateItemsStrategy

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer

class ALSWRJob extends MahoutJob {

  override def buildRecommender(dataModel: DataModel, seenDataModel: DataModel, args: Map[String, String]): Recommender = {

    val numFeatures: Int = getArg(args, "numFeatures").toInt
    val lambda: Double = getArg(args, "lambda").toDouble
    val numIterations: Int = getArg(args, "numIterations").toInt
    val unseenOnly: Boolean = getArgOpt(args, "unseenOnly", "false").toBoolean

    val factorizer: Factorizer = new ALSWRFactorizer(dataModel, numFeatures, lambda, numIterations)

    val candidateItemsStrategy = if (unseenOnly)
      new AllPreferredItemsNeighborhoodCandidateItemsStrategy(seenDataModel)
    else
      new AllPreferredItemsNeighborhoodCandidateItemsStrategy()

    // default stratagy is PreferredItemsNeighborhoodCandidateItemsStrategy();
    val recommender: Recommender = new SVDRecommender(dataModel, factorizer, candidateItemsStrategy)

    recommender
  }

}
