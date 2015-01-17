package org.template.similarproduct

import io.prediction.controller.LServing

import breeze.stats.mean
import breeze.stats.meanAndVariance
import breeze.stats.MeanAndVariance

class Serving
  extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query,
    predictedResults: Seq[PredictedResult]): PredictedResult = {

    // MODFIED
    // Standardize the score before combine
    val mvList: Seq[MeanAndVariance] = predictedResults.map { pr =>
      meanAndVariance(pr.itemScores.map(_.score))
    }

    val standardResults: Seq[Array[ItemScore]] = predictedResults.zipWithIndex
      .map { case (pr, i) =>
        pr.itemScores.map { is =>
          // standardize score (z-score)
          val score = (is.score - mvList(i).mean) / mvList(i).stdDev
          ItemScore(is.item, score)
        }
      }

    // sum the standardized score if same item
    val combined = standardResults.flatten // Array of ItemScore
      .groupBy(_.item) // groupBy item id
      .mapValues(itemScores => itemScores.map(_.score).reduce(_ + _))
      .toArray // array of (item id, score)
      .sortBy(_._2)(Ordering.Double.reverse)
      .take(query.num)
      .map { case (k,v) => ItemScore(k, v) }

    new PredictedResult(combined)
  }
}
