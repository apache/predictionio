package io.prediction.engines.itemrank.legacy

import io.prediction.controller.Params
import io.prediction.controller.LAlgorithm

import io.prediction.engines.itemrank.PreparedData
import io.prediction.engines.itemrank.Query
import io.prediction.engines.itemrank.Prediction
import io.prediction.engines.util.MahoutUtil

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

import com.github.nscala_time.time.Imports._

import scala.collection.JavaConversions._

class LegacyAlgorithmParams(
  val booleanData: Boolean = false,
  val itemSimilarity: String = "LogLikelihoodSimilarity",
  val weighted: Boolean = false,
  val threshold: Double = Double.MinPositiveValue,
  val nearestN: Int = 10,
  val unseenOnly: Boolean = false,
  val freshness: Int = 0,
  val freshnessTimeUnit: Int = 86400,
  val recommendationTime: Long = DateTime.now.millis
) extends Params {}

case class ItemModel(
  val id: String,
  val starttime: Long
) extends Serializable

case class UserModel(
  val index: Long
) extends Serializable

class LegacyAlgorithmModel(
  val dataModel: DataModel,
  val seenDataModel: DataModel,
  val validItemsMap: Map[Long, ItemModel],
  val usersMap: Map[String, UserModel],
  val itemCount: Int,
  val params: LegacyAlgorithmParams
) extends Serializable {

  @transient lazy val recommender: Recommender = buildRecommender()
  @transient lazy val freshnessRescorer = new FreshnessRescorer(
      params.freshness,
      params.recommendationTime,
      params.freshnessTimeUnit,
      validItemsMap)

  private def buildRecommender(): Recommender = {

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

  class FreshnessRescorer(freshness: Int, recommendationTime: Long,
    freshnessTimeUnit: Long,
    itemsMap: Map[Long, ItemModel]) extends IDRescorer {
    def isFiltered(id: Long): Boolean = false

    def rescore(id: Long, originalScore: Double): Double = {
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

/* Legacy Algo from 0.7 */
class LegacyAlgorithm(params: LegacyAlgorithmParams)
  extends LAlgorithm[LegacyAlgorithmParams, PreparedData,
  LegacyAlgorithmModel, Query, Prediction] {

  override def train(pareparedData: PreparedData): LegacyAlgorithmModel = {

    val dataModel: DataModel = if (params.booleanData) {
      MahoutUtil.buildBooleanPrefDataModel(pareparedData.rating.map { r =>
        (r.uindex, r.iindex, r.t) })
    } else {
      MahoutUtil.buildDataModel(pareparedData.rating.map{ r =>
        (r.uindex, r.iindex, r.rating.toFloat, r.t) })
    }

    // don't have seperated seen actions data for now
    val seenDataModel: DataModel = dataModel

    val validItemsMap: Map[Long, ItemModel] = pareparedData.items
      .map{ case (k, v) =>
        (k.toLong, ItemModel(
          v.iid,
          v.starttime.getOrElse(params.recommendationTime)))
      }
    val usersMap: Map[String, UserModel] = pareparedData.users
      .map {case (k,v) =>
        (v.uid, UserModel(k.toLong))
      }
    val itemCount = pareparedData.items.size

    new LegacyAlgorithmModel(
      dataModel,
      seenDataModel,
      validItemsMap,
      usersMap,
      itemCount,
      params
    )
  }

  override def predict(model: LegacyAlgorithmModel,
    query: Query): Prediction = {

    val recomender = model.recommender
    val uindex = model.usersMap(query.uid).index
    // List[RecommendedItem] // getItemID(), getValue()
    val rec = if (params.freshness != 0)
      recomender.recommend(uindex, model.itemCount, model.freshnessRescorer)
    else
      recomender.recommend(uindex, model.itemCount)

    val all: Seq[(String, Double)] = rec.toList.map { r =>
      val iid = model.validItemsMap(r.getItemID()).id
      (iid, r.getValue().toDouble)
    }
    // following adopted from 0.7 serving output logic
    /**
     * If no recommendations found for this user, simply return the original
     * list.
     */
    val iids = query.items
    val iidsSet = iids.toSet
    val ranked: Seq[(String, Double)] = all.filter( r =>
      iidsSet(r._1)
    )

    //val (rankedIids, rankedScores) = ranked.unzip
    val rankedIidsSet = ranked.map(_._1).toSet
    val unranked = iids.filterNot(x => rankedIidsSet(x))

    new Prediction(
      items = (ranked ++ (unranked.map(i => (i, Double.NaN)))),
      isOriginal = (ranked.size == 0)
    )
  }
}
