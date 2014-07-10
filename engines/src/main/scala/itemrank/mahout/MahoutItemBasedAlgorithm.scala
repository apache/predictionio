package io.prediction.engines.itemrank

import io.prediction.{ Algorithm }

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
import org.apache.mahout.cf.taste.impl.model.GenericPreference
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel
import org.apache.mahout.cf.taste.impl.common.FastByIDMap

import org.apache.mahout.cf.taste.impl.similarity.{
  CityBlockSimilarity,
  EuclideanDistanceSimilarity,
  LogLikelihoodSimilarity,
  PearsonCorrelationSimilarity,
  TanimotoCoefficientSimilarity,
  UncenteredCosineSimilarity
}

class MahoutItemBasedAlgorithm extends Algorithm[CleansedData,
  Feature, Prediction, MahoutItemBasedModel, MahoutItemBasedAlgoParams] {

  var _algoParams = new MahoutItemBasedAlgoParams(
    booleanData = true,
    itemSimilarity = "LogLikelihoodSimilarity",
    weighted = false,
    nearestN = 10,
    threshold = 5e-324,
    numSimilarItems = 50
  )
  var _minScore = 0.0

  override def init(params: MahoutItemBasedAlgoParams): Unit ={
    _algoParams = params;
  }

  override def train(
    cleansedData: CleansedData): MahoutItemBasedModel = {
      val numSimilarItems: Int = _algoParams.numSimilarItems
      val recommendationTime: Long = 0 // TODO: freshness for itemrank?
      val freshness = 0
      val freshnessTimeUnit: Long = 3600000 // 1 hour

      val dataModel: DataModel = buildDataModel(cleansedData.rating.map{ r =>
        (r.uindex, r.iindex, r.rating.toFloat)
      })
      val similarity: ItemSimilarity = buildItemSimilarity(dataModel)

      val itemIds = dataModel.getItemIDs.toSeq.map(_.toLong)
      val itemsMap = cleansedData.items
      val usersMap = cleansedData.users
      val validItemsSet = cleansedData.items.keySet

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
            (x._2 >= _algoParams.threshold) }

        (iid.toInt, getTopN(simScores, numSimilarItems)(ScoreOrdering.reverse))
      }.toMap

      val itemSimMap = allTopScores.seq.map {
        case (iindex, topScores) =>
          val topScoresIid = topScores.map{ case (simindex, score) =>
            (itemsMap(simindex).iid, score)
          }
          (itemsMap(iindex).iid, topScoresIid)
      }

      val userHistoryMap = cleansedData.rating.groupBy(_.uindex)
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
    feature: Feature): Prediction = {
      val nearestK = _algoParams.nearestN
      val uid = feature.uid
      val possibleItems = feature.items
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

  /* Build DataModel with Seq of (uid, iid, rating)
   */
  private def buildDataModel(ratingSeq: Seq[(Int, Int, Float)]): DataModel = {
    val allPrefs = new FastByIDMap[PreferenceArray]()
    ratingSeq.groupBy(_._1)
      .foreach { case (uid, ratingList) =>
        val prefList: Seq[(Preference, Int)] = ratingList.map{ r =>
          new GenericPreference(r._1.toLong, r._2.toLong, r._3)
        }.zipWithIndex

        val userPref = new GenericUserPreferenceArray(prefList.size)
        prefList.foreach { case (pref, i) =>
          userPref.set(i, pref)
        }

        allPrefs.put(uid.toLong, userPref)
      }
    new GenericDataModel(allPrefs)
  }

  object ScoreOrdering extends Ordering[(Int, Double)] {
    override def compare(a: (Int, Double), b: (Int, Double)) =
      a._2 compare b._2
  }

  def getTopN[T](s: Seq[T], n: Int)(ord: Ordering[T]): Seq[T] = {
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
  def buildItemSimilarity(dataModel: DataModel): ItemSimilarity = {

    val booleanData: Boolean = _algoParams.booleanData
    val itemSimilarity: String = _algoParams.itemSimilarity
    val weighted: Boolean = _algoParams.weighted

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
