package io.prediction.algorithms.mahout.itemrec.svdplusplus

import scala.collection.JavaConversions._

import io.prediction.algorithms.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer

class SVDPlusPlusJob extends MahoutJob {

  override def buildRecommender(dataModel: DataModel, args: Map[String, String]): Recommender = {

    val numFeatures: Int = getArg(args, "numFeatures").toInt
    val learningRate: Double = getArg(args, "learningRate").toDouble
    val preventOverfitting: Double = getArg(args, "preventOverfitting").toDouble
    val randomNoise: Double = getArg(args, "randomNoise").toDouble
    val numIterations: Int = getArg(args, "numIterations").toInt
    val learningRateDecay: Double = getArg(args, "learningRateDecay").toDouble

    val factorizer: Factorizer = new SVDPlusPlusFactorizer(dataModel, numFeatures, learningRate, preventOverfitting,
      randomNoise, numIterations, learningRateDecay)

    val recommender: Recommender = new SVDRecommender(dataModel, factorizer)

    recommender
  }
}
