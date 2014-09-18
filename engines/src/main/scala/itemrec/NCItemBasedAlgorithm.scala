package io.prediction.engines.itemrec

import io.prediction.engines.base.mahout.NCItemBasedAlgorithmModel
import io.prediction.engines.base

import org.apache.mahout.cf.taste.common.NoSuchUserException
import org.apache.mahout.cf.taste.recommender.RecommendedItem

import grizzled.slf4j.Logger

import com.github.nscala_time.time.Imports._

import scala.collection.JavaConversions._

case class NCItemBasedAlgorithmParams(
  val booleanData: Boolean = true,
  val itemSimilarity: String = "LogLikelihoodSimilarity",
  val weighted: Boolean = false,
  val threshold: Double = Double.MinPositiveValue,
  val nearestN: Int = 10,
  val unseenOnly: Boolean = false,
  val freshness: Int = 0,
  val freshnessTimeUnit: Int = 86400,
  val recommendationTime: Option[Long] = Some(DateTime.now.millis)
) extends base.mahout.AbstractItemBasedAlgorithmParams


class NCItemBasedAlgorithm(params: NCItemBasedAlgorithmParams)
  extends base.mahout.AbstractNCItemBasedAlgorithm[Query, Prediction](params) {

  override
  def predict(model: NCItemBasedAlgorithmModel,
    query: Query): Prediction = {

    val recomender = model.recommender
    val rec: List[RecommendedItem] = model.usersMap.get(query.uid)
      .map { user =>
        val uindex = user.index
        // List[RecommendedItem] // getItemID(), getValue()
        try {
          if (params.freshness != 0)
            recomender.recommend(uindex, query.n,
              model.freshnessRescorer).toList
          else
            recomender.recommend(uindex, query.n).toList
        } catch {
          case e: NoSuchUserException => {
            logger.info(
              s"NoSuchUserException ${query.uid} (index ${uindex}) in model.")
            List()
          }
          case e: Throwable => throw new RuntimeException(e)
        }
      }.getOrElse{
        logger.info(s"Unknow user id ${query.uid}")
        List()
      }

    val items: Seq[(String, Double)] = rec.map { r =>
      val iid = model.validItemsMap(r.getItemID()).id
      (iid, r.getValue().toDouble)
    }

    new Prediction(
      items = items
    )
  }
}
