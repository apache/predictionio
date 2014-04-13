package io.prediction.algorithms.mahout.itemrec.knnuserbased

import scala.collection.JavaConversions._

import io.prediction.algorithms.mahout.itemrec.MahoutJob
import io.prediction.algorithms.mahout.itemrec.{ UserBasedRecommender, BooleanPrefUserBasedRecommender }

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.recommender.RecommendedItem
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood
import org.apache.mahout.cf.taste.similarity.UserSimilarity
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood
import org.apache.mahout.cf.taste.impl.similarity.{
  CityBlockSimilarity,
  EuclideanDistanceSimilarity,
  LogLikelihoodSimilarity,
  PearsonCorrelationSimilarity,
  SpearmanCorrelationSimilarity,
  TanimotoCoefficientSimilarity,
  UncenteredCosineSimilarity
}

class KNNUserBasedJob extends MahoutJob {

  val userSimilarityValues = Seq(
    "CityBlockSimilarity",
    "EuclideanDistanceSimilarity",
    "LogLikelihoodSimilarity",
    "PearsonCorrelationSimilarity",
    "SpearmanCorrelationSimilarity",
    "TanimotoCoefficientSimilarity",
    "UncenteredCosineSimilarity")

  val defaultUserSimilarity = "PearsonCorrelationSimilarity"

  override def buildRecommender(dataModel: DataModel, seenDataModel: DataModel, args: Map[String, String]): Recommender = {

    val booleanData: Boolean = getArgOpt(args, "booleanData", "false").toBoolean
    val nearestN: Int = getArgOpt(args, "nearestN", "10").toInt
    val userSimilarity: String = getArgOpt(args, "userSimilarity", defaultUserSimilarity)
    val weighted: Boolean = getArgOpt(args, "weighted", "false").toBoolean
    val minSimilarity: Double = getArgOpt(args, "minSimilarity").map(_.toDouble).getOrElse(Double.NegativeInfinity)
    val samplingRate: Double = getArgOpt(args, "samplingRate", "1.0").toDouble
    val unseenOnly: Boolean = getArgOpt(args, "unseenOnly", "false").toBoolean

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

    val neighborhood: UserNeighborhood = new NearestNUserNeighborhood(nearestN, minSimilarity, similarity, dataModel, samplingRate)

    val recSeenDataModel = if (unseenOnly) seenDataModel else null

    val recommender: Recommender = if (booleanData) {
      new BooleanPrefUserBasedRecommender(dataModel, neighborhood, similarity,
        recSeenDataModel)
    } else {
      new UserBasedRecommender(dataModel, neighborhood, similarity,
        recSeenDataModel)
    }

    recommender
  }
}
