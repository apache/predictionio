package org.template.recommendation

import breeze.stats.MeanAndVariance
import breeze.stats.meanAndVariance
import io.prediction.controller.LServing

class Serving
  extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query,
            predictedResults: Seq[PredictedResult]): PredictedResult = {

    val standard: Seq[Array[ItemScore]] = if (query.num == 1) {
      // if query 1 item, don't standardize
      predictedResults.map(_.itemScores)
    } else {
      // Standardize the score before combine
      val mvList: Seq[MeanAndVariance] = predictedResults.map { pr ⇒
        meanAndVariance(pr.itemScores.map(_.score))
      }

      predictedResults.zipWithIndex.map { case (pr, i) ⇒
        pr.itemScores.map { is ⇒
          // standardize score (z-score)
          // if standard deviation is 0 (when all items have the same score,
          // meaning all items are ranked equally), return 0.
          val score =
            if (mvList(i).stdDev == 0) { 0 }
            else { (is.score - mvList(i).mean) / mvList(i).stdDev }

          ItemScore(is.item, score, is.creationYear)
        }
      }
    }

    // sum the standardized score if same item
    val combined = standard.flatten // Array of ItemScore
      .groupBy(_.item) // groupBy item id
      .mapValues{ itemScores ⇒
      itemScores.map(_.score).reduce(_ + _) →
        itemScores.headOption.flatMap(_.creationYear)
      }
      .toArray // array of (item id, score, creationYear)
      // HOWTO: Here resulting set could be sorted by field from qeury
      .sortBy{ case (_, (_, year)) ⇒ year }(Ordering.Option[Int].reverse)
      .take(query.num)
      .map { case (item,(score, year)) ⇒ ItemScore(item, score, year) }

    new PredictedResult(combined)
  }

}
