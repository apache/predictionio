package io.prediction.algorithms.mahout.itemrec.knnitembased

import io.prediction.algorithms.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.similarity.ItemSimilarity
import org.apache.mahout.cf.taste.impl.recommender.{ GenericItemBasedRecommender, GenericBooleanPrefItemBasedRecommender }
import org.apache.mahout.cf.taste.impl.similarity.{
  CityBlockSimilarity,
  EuclideanDistanceSimilarity,
  LogLikelihoodSimilarity,
  PearsonCorrelationSimilarity,
  TanimotoCoefficientSimilarity,
  UncenteredCosineSimilarity
}

class KNNItemBasedJob extends MahoutJob {

  val defaultItemSimilarity = "LogLikelihoodSimilarity"

  override def buildRecommender(dataModel: DataModel, args: Map[String, String]): Recommender = {

    val booleanData: Boolean = getArgOpt(args, "booleanData", "false").toBoolean
    val itemSimilarity: String = getArgOpt(args, "itemSimilarity", defaultItemSimilarity)
    val weighted: Boolean = getArgOpt(args, "weighted", "false").toBoolean

    val weightedParam: Weighting = if (weighted) Weighting.WEIGHTED else Weighting.UNWEIGHTED

    val similarity: ItemSimilarity = itemSimilarity match {
      case "CityBlockSimilarity" => new CityBlockSimilarity(dataModel)
      case "EuclideanDistanceSimilarity" => new EuclideanDistanceSimilarity(dataModel, weightedParam)
      case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(dataModel)
      case "PearsonCorrelationSimilarity" => new PearsonCorrelationSimilarity(dataModel, weightedParam)
      case "TanimotoCoefficientSimilarity" => new TanimotoCoefficientSimilarity(dataModel)
      case "UncenteredCosineSimilarity" => new UncenteredCosineSimilarity(dataModel, weightedParam)
      case _ => throw new RuntimeException("Invalid ItemSimilarity: " + itemSimilarity)
    }

    // As of Mahout 0.9, the implementation uses ALL neighbours (k=ALL)
    val recommender: Recommender = if (booleanData) {
      new GenericBooleanPrefItemBasedRecommender(dataModel, similarity)
    } else {
      new GenericItemBasedRecommender(dataModel, similarity)
    }

    recommender
  }

}