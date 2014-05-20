package io.prediction.engines.itemrank

import io.prediction.{ Algorithm }
import breeze.linalg.{ SparseVector }

class KNNAlgorithm extends Algorithm[TrainigData, Feature, Target, KNNModel, KNNAlgoParams] {

  override def init(algoParams: KNNAlgoParams): Unit = {} // TODO

  override def train(trainingData: TrainigData): KNNModel = {
    val rating = trainingData.rating

    if (!rating.isEmpty) {
      val numOfUsers = trainingData.users.size
      val items = trainingData.items
      val users = trainingData.users

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

            (users(k), history.toSet)
        }

      val seenMap = trainingData.seen.groupBy(_._1)
        .map {
          case (k, listOfSeen) =>
            val seen = listOfSeen.map(s => items(s._2).iid).toSet
            (users(k), seen)
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
        val score = cosineSimilarity(v1, v2)
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
        userSeen = seenMap,
        userHistory = userHistoryMap,
        itemSim = itemSimMap
      )
    } else {
      new KNNModel(
        userSeen = Map(),
        userHistory = Map(),
        itemSim = Map()
      )
    }
  }

  override def predict(model: KNNModel, feature: Feature): Target = {
    val nearestK = 10 // TODO: algo param
    val uid = feature.uid
    val possibleItems = feature.items
    val history: Map[String, Int] = model.userHistory.getOrElse(uid, Set())
      .toMap

    val rankedItems = possibleItems.toSeq.map { iid =>
      val score = if (model.itemSim.contains(iid) && (!history.isEmpty)) {
        val sims: Seq[(String, Double)] = model.itemSim(iid)
        val rank = sims.filter { case (iid, score) => history.contains(iid) }
          .take(nearestK)
          .map { case (iid, score) => (score * history(iid)) }
          .sum
        rank
      } else {
        0.0
      }
      (iid, score)
    }.sortBy(_._2)(Ordering.Double.reverse)
    // TODO: keep same order as input if no score

    new Target(
      items = rankedItems
    )
  }

  private def cosineSimilarity(v1: SparseVector[Int],
    v2: SparseVector[Int]): Double =
    {
      val p = v1 dot v2
      val v1L2Norm = Math.sqrt((v1 :* v1).sum)
      val v2L2Norm = Math.sqrt((v2 :* v2).sum)
      p.toDouble / (v1L2Norm * v2L2Norm)
    }
}
