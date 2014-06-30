package io.prediction.engines.itemrank

import io.prediction.{ Algorithm }
import breeze.linalg.{ SparseVector, sum => LSum }

class KNNAlgorithm extends Algorithm[TrainingData, Feature, Prediction,
  KNNModel, KNNAlgoParams] {

  var _k = 10
  var _similarity = "cosine"
  var _minScore = 0.0

  override def init(algoParams: KNNAlgoParams): Unit = {
    _k = algoParams.k
    _similarity = algoParams.similarity
    _similarity match {
      case "cosine" => _minScore = 0.0
      case "jaccard" => _minScore = 0.0
    }
  }

  override def train(trainingData: TrainingData): KNNModel = {
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
        val score = _similarity match {
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
        //userSeen = seenMap,
        userHistory = userHistoryMap,
        itemSim = itemSimMap
      )
    } else {
      new KNNModel(
        //userSeen = Map(),
        userHistory = Map(),
        itemSim = Map()
      )
    }
  }

  override def predict(model: KNNModel, feature: Feature): Prediction = {
    val nearestK = _k
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
