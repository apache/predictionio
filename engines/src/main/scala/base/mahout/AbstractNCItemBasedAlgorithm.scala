package io.prediction.engines.base.mahout

import io.prediction.controller.Params
import io.prediction.controller.LAlgorithm

//import io.prediction.engines.itemrank.Query
//import io.prediction.engines.itemrank.Prediction
import io.prediction.engines.util.MahoutUtil
//import io.prediction.engines.base.mahout.KNNItemBasedRecommender
//import io.prediction.engines.base.mahout.AllValidItemsCandidateItemsStrategy
import io.prediction.engines.base.PreparedData

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.recommender.IDRescorer
import org.apache.mahout.cf.taste.similarity.ItemSimilarity
import org.apache.mahout.cf.taste.impl.similarity.{
  CityBlockSimilarity,
  EuclideanDistanceSimilarity,
  LogLikelihoodSimilarity,
  PearsonCorrelationSimilarity,
  TanimotoCoefficientSimilarity,
  UncenteredCosineSimilarity
}
import org.apache.mahout.cf.taste.common.NoSuchUserException
import org.apache.mahout.cf.taste.recommender.RecommendedItem

import grizzled.slf4j.Logger

import com.github.nscala_time.time.Imports._

import scala.collection.JavaConversions._

abstract class AbstractItemBasedAlgorithmParams extends Params {
  val booleanData: Boolean
  val itemSimilarity: String
  val weighted: Boolean
  val threshold: Double
  val nearestN: Int
  val unseenOnly: Boolean
  val freshness: Int
  val freshnessTimeUnit: Int
  val recommendationTime: Option[Long]
}

case class ItemModel(
  val id: String,
  val starttime: Long
) extends Serializable

case class UserModel(
  val index: Long
) extends Serializable

class NCItemBasedAlgorithmModel(
  val dataModel: DataModel,
  val seenDataModel: DataModel,
  val validItemsMap: Map[Long, ItemModel],
  val usersMap: Map[String, UserModel],
  val itemCount: Int, // TODO: delete this
  val params: AbstractItemBasedAlgorithmParams
) extends Serializable {

  @transient lazy val logger = Logger[this.type]
  @transient lazy val recommender: Recommender = buildRecommender()
  @transient lazy val freshnessRescorer = new FreshnessRescorer(
      params.freshness,
      params.recommendationTime,
      params.freshnessTimeUnit,
      validItemsMap)

  private def buildRecommender(): Recommender = {
    logger.info("Building recommender...")
    val weightedParam: Weighting = if (params.weighted) Weighting.WEIGHTED
      else Weighting.UNWEIGHTED

    val similarity: ItemSimilarity = params.itemSimilarity match {
      case "CityBlockSimilarity" =>
        new CityBlockSimilarity(dataModel)
      case "EuclideanDistanceSimilarity" =>
        new EuclideanDistanceSimilarity(dataModel, weightedParam)
      case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(dataModel)
      case "PearsonCorrelationSimilarity" =>
        new PearsonCorrelationSimilarity(dataModel, weightedParam)
      case "TanimotoCoefficientSimilarity" =>
        new TanimotoCoefficientSimilarity(dataModel)
      case "UncenteredCosineSimilarity" =>
        new UncenteredCosineSimilarity(dataModel, weightedParam)
      case _ => throw new RuntimeException("Invalid ItemSimilarity: " +
        params.itemSimilarity)
    }

    val candidateItemsStrategy = if (params.unseenOnly)
      new AllValidItemsCandidateItemsStrategy(validItemsMap.keySet.toArray,
        seenDataModel)
    else
      new AllValidItemsCandidateItemsStrategy(validItemsMap.keySet.toArray)

    val recommender: Recommender = new KNNItemBasedRecommender(
      dataModel,
      similarity,
      candidateItemsStrategy,
      params.booleanData,
      params.nearestN,
      params.threshold)

    recommender
  }

  class FreshnessRescorer(freshness: Int, recommendationTimeOpt: Option[Long],
    freshnessTimeUnit: Long,
    itemsMap: Map[Long, ItemModel]) extends IDRescorer {

    logger.info("Building FreshnessRescorer...")

    def isFiltered(id: Long): Boolean = false

    def rescore(id: Long, originalScore: Double): Double = {

      val recommendationTime = recommendationTimeOpt.getOrElse(
        DateTime.now.millis)

      if (freshness > 0) {
        itemsMap.get(id) map { i =>
          val timeDiff = (recommendationTime - i.starttime) / 1000 /
            freshnessTimeUnit
          if (timeDiff > 0)
            originalScore * scala.math.exp(-timeDiff / (11 - freshness))
          else
            originalScore
        } getOrElse originalScore
      } else originalScore
    }
  }
}

abstract class AbstractNCItemBasedAlgorithm[Q : Manifest, P](
  params: AbstractItemBasedAlgorithmParams)
  extends LAlgorithm[AbstractItemBasedAlgorithmParams, PreparedData,
  NCItemBasedAlgorithmModel, Q, P] {

  @transient lazy val logger = Logger[this.type]
  @transient lazy val recommendationTime = params.recommendationTime.getOrElse(
    DateTime.now.millis)

  override def train(preparedData: PreparedData): NCItemBasedAlgorithmModel = {

    val dataModel: DataModel = if (params.booleanData) {
      MahoutUtil.buildBooleanPrefDataModel(preparedData.rating.map { r =>
        (r.uindex, r.iindex, r.t) })
    } else {
      MahoutUtil.buildDataModel(preparedData.rating.map{ r =>
        (r.uindex, r.iindex, r.rating.toFloat, r.t) })
    }

    // don't have seperated seen actions data for now
    val seenDataModel: DataModel = dataModel

    val validItemsMap: Map[Long, ItemModel] = preparedData.items
      .map{ case (k, v) =>
        (k.toLong, ItemModel(
          v.iid,
          v.starttime.getOrElse(recommendationTime))
        )
      }
    val usersMap: Map[String, UserModel] = preparedData.users
      .map {case (k,v) =>
        (v.uid, UserModel(k.toLong))
      }
    val itemCount = preparedData.items.size

    new NCItemBasedAlgorithmModel(
      dataModel,
      seenDataModel,
      validItemsMap,
      usersMap,
      itemCount,
      params
    )
  }

}
