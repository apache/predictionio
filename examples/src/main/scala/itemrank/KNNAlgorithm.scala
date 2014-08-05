package io.prediction.examples.itemrank

import io.prediction.controller.LAlgorithm
import breeze.linalg.{ SparseVector, sum => LSum }

class KNNAlgorithm(params: KNNAlgoParams)
  extends LAlgorithm[KNNAlgoParams, PreparedData, KNNModel, Query,
  Prediction] {

  val _minScore = 0.0

  override def train(preparedData: PreparedData): KNNModel = {
    val rating = preparedData.rating

    if (!rating.isEmpty) {
      val numOfUsers = preparedData.users.size
      val items = preparedData.items
      val users = preparedData.users

      val itemVectorMap = rating.groupBy(_.iindex)
        .mapValues { listOfRating =>
          val sorted: Array[(Int, Int)] = listOfRating.sortBy(_.uindex)
            .map(r => (r.uindex, r.rating)).toArray
          new SparseVector(sorted.map(_._1 - 1),
            // index -1 because uindex starts from 1
            sorted.map(_._2), // data
            sorted.size, numOfUsers)
        }

      val userHistoryMap = rating.groupBy(_.uindex)
        .map {
          case (k, listOfRating) =>
            val history = listOfRating.map(r => (items(r.iindex).iid, r.rating))

            (users(k).uid, history.toSet)
        }

      val sortedItemIndex = itemVectorMap.keys.toSeq.sorted
      val numOfVectors = itemVectorMap.size
      val numOfItems = sortedItemIndex.last

      val halfScore = (for (
        i <- 0 until numOfVectors;
        j <- i + 1 until numOfVectors
      ) yield {
        val i1Index = sortedItemIndex(i)
        val i2Index = sortedItemIndex(j)
        val v1 = itemVectorMap(i1Index)
        val v2 = itemVectorMap(i2Index)
        val score = params.similarity match {
          case "cosine" => cosineSimilarity(v1, v2)
          case "jaccard" => jaccardSimilarity(v1, v2)
          case _ => 0.0
        }

        (i1Index, i2Index, score)
      }).filter(_._3 > 0)

      val fullScore = halfScore ++ (halfScore.map {
        case (i1Index, i2Index, score) => (i2Index, i1Index, score)
      })

      val itemSimMap = fullScore.groupBy(_._1)
        .mapValues { listOfScore =>
          listOfScore.sortBy(_._3)(Ordering.Double.reverse)
            .map {
              case (i1, i2, score) =>
                (items(i2).iid, score)
            }
        }.map { case (k, v) => (items(k).iid, v) }

      new KNNModel(
        userHistory = userHistoryMap,
        itemSim = itemSimMap
      )
    } else {
      new KNNModel(
        userHistory = Map(),
        itemSim = Map()
      )
    }
  }

  override def predict(model: KNNModel, query: Query): Prediction = {
    val nearestK = params.k
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

  private def cosineSimilarity(v1: SparseVector[Int],
    v2: SparseVector[Int]): Double =
    {
      val p = v1 dot v2
      val v1L2Norm = Math.sqrt(LSum(v1 :* v1))
      val v2L2Norm = Math.sqrt(LSum(v2 :* v2))
      p.toDouble / (v1L2Norm * v2L2Norm)
    }

  private def jaccardSimilarity(v1: SparseVector[Int],
    v2: SparseVector[Int]): Double = {
      val set1 = v1.index.toSet
      val set2 = v2.index.toSet
      val intersectSize = set1.intersect(set2).size
      val unionSize = set1.size + set2.size - intersectSize
      intersectSize.toDouble / unionSize
    }
}
