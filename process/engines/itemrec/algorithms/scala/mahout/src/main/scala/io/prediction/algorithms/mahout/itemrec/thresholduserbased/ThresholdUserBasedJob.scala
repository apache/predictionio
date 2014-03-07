package io.prediction.algorithms.mahout.itemrec.thresholduserbased

import scala.collection.JavaConversions._

import io.prediction.algorithms.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.recommender.RecommendedItem
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood
import org.apache.mahout.cf.taste.similarity.UserSimilarity
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.impl.recommender.{ GenericUserBasedRecommender, GenericBooleanPrefUserBasedRecommender }
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood
import org.apache.mahout.cf.taste.impl.similarity.{
  CityBlockSimilarity,
  EuclideanDistanceSimilarity,
  LogLikelihoodSimilarity,
  PearsonCorrelationSimilarity,
  SpearmanCorrelationSimilarity,
  TanimotoCoefficientSimilarity,
  UncenteredCosineSimilarity
}

class ThresholdUserBasedJob extends MahoutJob {

  val userSimilarityValues = Seq(
    "CityBlockSimilarity",
    "EuclideanDistanceSimilarity",
    "LogLikelihoodSimilarity",
    "PearsonCorrelationSimilarity",
    "SpearmanCorrelationSimilarity",
    "TanimotoCoefficientSimilarity",
    "UncenteredCosineSimilarity")

  val defaultUserSimilarity = "PearsonCorrelationSimilarity"

  override def buildRecommender(dataModel: DataModel, args: Map[String, String]): Recommender = {

    val booleanData: Boolean = getArgOpt(args, "booleanData", "false").toBoolean
    val userSimilarity: String = getArgOpt(args, "userSimilarity", defaultUserSimilarity)
    val weighted: Boolean = getArgOpt(args, "weighted", "false").toBoolean
    val threshold: Double = getArgOpt(args, "threshold").map(_.toDouble).getOrElse(Double.MinPositiveValue)
    val samplingRate: Double = getArgOpt(args, "samplingRate", "1.0").toDouble

    val weightedParam: Weighting = if (weighted) Weighting.WEIGHTED else Weighting.UNWEIGHTED

    val similarity: UserSimilarity = userSimilarity match {
      case "CityBlockSimilarity" => new CityBlockSimilarity(dataModel)
      case "EuclideanDistanceSimilarity" => new EuclideanDistanceSimilarity(dataModel, weightedParam)
      case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(dataModel)
      case "PearsonCorrelationSimilarity" => new PearsonCorrelationSimilarity(dataModel, weightedParam)
      case "SpearmanCorrelationSimilarity" => new SpearmanCorrelationSimilarity(dataModel)
      case "TanimotoCoefficientSimilarity" => new TanimotoCoefficientSimilarity(dataModel)
      case "UncenteredCosineSimilarity" => new UncenteredCosineSimilarity(dataModel, weightedParam)
      case _ => throw new RuntimeException("Invalid UserSimilarity: " + userSimilarity)
    }

    val neighborhood: UserNeighborhood = new ThresholdUserNeighborhood(threshold, similarity, dataModel, samplingRate)

    val recommender: Recommender = if (booleanData) {
      new GenericBooleanPrefUserBasedRecommender(dataModel, neighborhood, similarity)
    } else {
      new GenericUserBasedRecommender(dataModel, neighborhood, similarity)
    }

    recommender
  }

}
