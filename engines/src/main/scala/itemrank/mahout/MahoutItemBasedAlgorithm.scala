package io.prediction.engines.itemrank

import io.prediction.controller.LAlgorithm

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import scala.collection.JavaConversions._

import scala.collection.mutable.PriorityQueue

import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.similarity.ItemSimilarity
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.model.Preference
import org.apache.mahout.cf.taste.model.PreferenceArray
import org.apache.mahout.cf.taste.impl.model.GenericDataModel
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel
import org.apache.mahout.cf.taste.impl.model.GenericPreference
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel
import org.apache.mahout.cf.taste.impl.common.FastByIDMap
import org.apache.mahout.cf.taste.impl.common.FastIDSet

import org.apache.mahout.cf.taste.impl.similarity.{
  CityBlockSimilarity,
  EuclideanDistanceSimilarity,
  LogLikelihoodSimilarity,
  PearsonCorrelationSimilarity,
  TanimotoCoefficientSimilarity,
  UncenteredCosineSimilarity
}

class MahoutItemBasedAlgorithm(params: MahoutItemBasedAlgoParams)
  extends LAlgorithm[MahoutItemBasedAlgoParams, PreparedData,
  MahoutItemBasedModel, Query, Prediction] {

  var _minScore = 0.0

  override def train(
    pareparedData: PreparedData): MahoutItemBasedModel = {
      val numSimilarItems: Int = params.numSimilarItems
      val recommendationTime: Long = 0 // TODO: freshness for itemrank?
      val freshness = 0
      val freshnessTimeUnit: Long = 3600000 // 1 hour

      val dataModel: DataModel = if (params.booleanData) {
        buildBooleanPrefDataModel(pareparedData.rating.map { r =>
          (r.uindex, r.iindex, r.t) })
      } else {
        buildDataModel(pareparedData.rating.map{ r =>
          (r.uindex, r.iindex, r.rating.toFloat, r.t) })
      }
      val similarity: ItemSimilarity = buildItemSimilarity(dataModel)

      val itemIds = dataModel.getItemIDs.toSeq.map(_.toLong)
      val itemsMap = pareparedData.items
      val usersMap = pareparedData.users
      val validItemsSet = pareparedData.items.keySet

      val allTopScores = itemIds.par.map { iid =>
        val simScores = validItemsSet.toSeq
          .map { simiid =>
            val originalScore = similarity.itemSimilarity(iid, simiid)
            val score = if (freshness > 0) {
              itemsMap.get(simiid) map { item =>
                item.starttime.map { starttime =>
                  val timeDiff = (recommendationTime - starttime) / 1000.0 /
                    freshnessTimeUnit
                  if (timeDiff > 0)
                    originalScore * scala.math.exp(-timeDiff / (11 - freshness))
                  else
                    originalScore
                } getOrElse originalScore
              } getOrElse originalScore
            } else originalScore
            (simiid, score)
          }
          // filter out invalid score or the same iid itself, < threshold
          .filter { x: (Int, Double) => (!x._2.isNaN()) && (x._1 != iid) &&
            (x._2 >= params.threshold) }

        (iid.toInt, getTopN(simScores, numSimilarItems)(ScoreOrdering.reverse))
      }.toMap

      val itemSimMap = allTopScores.seq.map {
        case (iindex, topScores) =>
          val topScoresIid = topScores.map{ case (simindex, score) =>
            (itemsMap(simindex).iid, score)
          }
          (itemsMap(iindex).iid, topScoresIid)
      }

      val userHistoryMap = pareparedData.rating.groupBy(_.uindex)
        .map {
          case (k, listOfRating) =>
            val history = listOfRating.map(r =>
              (itemsMap(r.iindex).iid, r.rating))

            (usersMap(k).uid, history.toSet)
        }

      new MahoutItemBasedModel (
        userHistory = userHistoryMap,
        itemSim = itemSimMap
      )
  }

  override def predict(model: MahoutItemBasedModel,
    query: Query): Prediction = {
      val nearestK = params.nearestN
      val uid = query.uid
      val possibleItems = query.items
      val history: Map[String, Int] = model.userHistory.getOrElse(uid, Set())
        .toMap

      val itemsScore = possibleItems.toSeq.map { iid =>
        val score = if (model.itemSim.contains(iid) && (!history.isEmpty)) {
          val sims: Seq[(String, Double)] = model.itemSim(iid)
          val rank = sims.filter { case (iid, score) => history.contains(iid) }
            .take(nearestK)
            .map { case (iid, score) => (score * history(iid)) }
            .sum
          Some(rank)
        } else {
          None
        }
        (iid, score)
      }

      // keep same order as input if no score
      val (itemsWithScore, itemsNoScore) = itemsScore.partition(_._2 != None)
      val sorted = itemsWithScore.map{case(iid, score) => (iid, score.get)}
        .sortBy(_._2)(Ordering.Double.reverse)

      val rankedItems = sorted ++
        (itemsNoScore.map{ case (iid, score) => (iid, _minScore) })

      new Prediction (
        items = rankedItems
      )
    }

  /* Build DataModel with Seq of (uid, iid, rating, timestamp)
   * NOTE: assume no duplicated rating on same iid by the same user
   */
  private def buildDataModel(
    ratingSeq: Seq[(Int, Int, Float, Long)]): DataModel = {

    val allPrefs = new FastByIDMap[PreferenceArray]()
    val allTimestamps = new FastByIDMap[FastByIDMap[java.lang.Long]]()

    ratingSeq.groupBy(_._1)
      .foreach { case (uid, ratingList) =>
        val userID = uid.toLong
        // preference of items for this user
        val userPrefs = new GenericUserPreferenceArray(ratingList.size)
        // timestamp of items for this user
        val userTimestamps = new FastByIDMap[java.lang.Long]()

        ratingList.zipWithIndex
          .foreach { case (r, i) =>
            val itemID = r._2.toLong
            val pref = new GenericPreference(userID, itemID, r._3)
            userPrefs.set(i, pref)
            userTimestamps.put(itemID, r._4)
        }

        allPrefs.put(userID, userPrefs)
        allTimestamps.put(userID, userTimestamps)
      }

    new GenericDataModel(allPrefs, allTimestamps)
  }

  /* Build DataModel with Seq of (uid, iid, timestamp)
   * NOTE: assume no duplicated iid by the same user
   */
  private def buildBooleanPrefDataModel(
    ratingSeq: Seq[(Int, Int, Long)]): DataModel = {

    val allPrefs = new FastByIDMap[FastIDSet]()
    val allTimestamps = new FastByIDMap[FastByIDMap[java.lang.Long]]()

    ratingSeq.foreach { case (uid, iid, t) =>
      val userID = uid.toLong
      val itemID = iid.toLong

      // item
      val idSet = allPrefs.get(userID)
      if (idSet == null) {
        val newIdSet = new FastIDSet()
        newIdSet.add(itemID)
        allPrefs.put(userID, newIdSet)
      } else {
        idSet.add(itemID)
      }
      // timestamp
      val timestamps = allTimestamps.get(userID)
      if (timestamps == null) {
        val newTimestamps = new FastByIDMap[java.lang.Long]
        newTimestamps.put(itemID, t)
        allTimestamps.put(userID, newTimestamps)
      } else {
        timestamps.put(itemID, t)
      }
    }

    new GenericBooleanPrefDataModel(allPrefs, allTimestamps)
  }

  object ScoreOrdering extends Ordering[(Int, Double)] {
    override def compare(a: (Int, Double), b: (Int, Double)) =
      a._2 compare b._2
  }

  private def getTopN[T](s: Seq[T], n: Int)(ord: Ordering[T]): Seq[T] = {
    val q = PriorityQueue()(ord)

    for (x <- s) {
      if (q.size < n)
        q.enqueue(x)
      else {
        // q is full
        if (ord.compare(x, q.head) < 0) {
          q.dequeue()
          q.enqueue(x)
        }
      }
    }

    q.dequeueAll.toSeq.reverse
  }

  // return Mahout's ItemSimilarity object
  private def buildItemSimilarity(dataModel: DataModel): ItemSimilarity = {

    val booleanData: Boolean = params.booleanData
    val itemSimilarity: String = params.itemSimilarity
    val weighted: Boolean = params.weighted

    val weightedParam: Weighting = if (weighted)
      Weighting.WEIGHTED
    else
      Weighting.UNWEIGHTED

    val similarity: ItemSimilarity = itemSimilarity match {
      case "CityBlockSimilarity" => new CityBlockSimilarity(dataModel)
      case "EuclideanDistanceSimilarity" =>
        new EuclideanDistanceSimilarity(dataModel, weightedParam)
      case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(dataModel)
      case "PearsonCorrelationSimilarity" =>
        new PearsonCorrelationSimilarity(dataModel, weightedParam)
      case "TanimotoCoefficientSimilarity" =>
        new TanimotoCoefficientSimilarity(dataModel)
      case "UncenteredCosineSimilarity" =>
        new UncenteredCosineSimilarity(dataModel, weightedParam)
      case _ => throw new RuntimeException(
        "Invalid ItemSimilarity: " + itemSimilarity)
    }

    similarity
  }

}
