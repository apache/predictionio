package org.template.recommendation

import io.prediction.controller.LServing

class Serving extends LServing[Query, PredictedResult] {

  override def serve(query: Query,
            predictedResults: Seq[PredictedResult]): PredictedResult =
    predictedResults.headOption.map { result ⇒
      val preparedItems = result.itemScores
        .groupBy(_.item)
        .mapValues { itemScores ⇒
          itemScores.map(_.score).reduce(_ + _) →
            itemScores.headOption.flatMap(_.creationYear)
        }.toArray
        .sortBy{ case (_, (_, year)) ⇒ year }(Ordering.Option[Int].reverse)
        .take(query.num)
        .map { case (item,(score, year)) ⇒ ItemScore(item, score, year) }
      new PredictedResult(preparedItems)

    }.getOrElse(new PredictedResult(Array.empty[ItemScore]))
}
