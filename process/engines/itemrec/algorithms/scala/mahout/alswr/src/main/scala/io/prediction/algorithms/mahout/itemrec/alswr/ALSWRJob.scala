package io.prediction.algorithms.mahout.itemrec.alswr

import scala.collection.JavaConversions._

import io.prediction.algorithms.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer

class ALSWRJob extends MahoutJob {

  override def buildRecommender(dataModel: DataModel, args: Map[String, String]): Recommender = {

    val numFeatures: Int = getArg(args, "numFeatures").toInt
    val lambda: Double = getArg(args, "lambda").toDouble
    val numIterations: Int = getArg(args, "numIterations").toInt

    val factorizer: Factorizer = new ALSWRFactorizer(dataModel, numFeatures, lambda, numIterations)

    // default stratagy is PreferredItemsNeighborhoodCandidateItemsStrategy();
    val recommender: Recommender = new SVDRecommender(dataModel, factorizer)

    recommender
  }

}