package org.template.recommendation

import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params
import io.prediction.data.storage.BiMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.{Rating => MLlibRating, CategoriesALSModels, ALS, ALSModel}

import grizzled.slf4j.Logger

case class ALSAlgorithmParams(
  rank: Int,
  numIterations: Int,
  lambda: Double,
  seed: Option[Long]) extends Params

class ALSAlgorithm(val ap: ALSAlgorithmParams)
  extends PAlgorithm[PreparedData, CategoriesALSModels, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): CategoriesALSModels = {
    // MLLib ALS cannot handle empty training data.
    require(data.ratings.take(1).nonEmpty,
      s"RDD[Rating] in PreparedData cannot be empty." +
      " Please check if DataSource generates TrainingData" +
      " and Preprator generates PreparedData correctly.")
    // Convert user and item String IDs to Int index for MLlib
    val userStringIntMap = BiMap.stringInt(data.ratings.map(_.user))
    val itemStringIntMap = BiMap.stringInt(data.ratings.map(_.item))
    val mllibRatings = data.ratings.map( r =>
      // MLlibRating requires integer index for user and item
      MLlibRating(userStringIntMap(r.user), itemStringIntMap(r.item), r.rating)
    )

    // seed for MLlib ALS
    val seed = ap.seed.getOrElse(System.nanoTime)

    // If you only have one type of implicit event (Eg. "view" event only),
    // replace ALS.train(...) with
    //val m = ALS.trainImplicit(
      //ratings = mllibRatings,
      //rank = ap.rank,
      //iterations = ap.numIterations,
      //lambda = ap.lambda,
      //blocks = -1,
      //alpha = 1.0,
      //seed = seed)

    val m = ALS.train(
      ratings = mllibRatings,
      rank = ap.rank,
      iterations = ap.numIterations,
      lambda = ap.lambda,
      blocks = -1,
      seed = seed)

    val categories = data.items.flatMap(_.categories).distinct().collect().toSet

    val categoriesMap = categories.map { category =>
      val itemIds = data.items
        .filter(_.categories.contains(category))
        .map(item => itemStringIntMap(item.id))
        .collect()
        .toSet

      val itemFeatures = m.productFeatures.filter {
        case (id, features) => itemIds.contains(id)
      }

      category -> new ALSModel(
        rank = m.rank,
        userFeatures = m.userFeatures,
        productFeatures = itemFeatures
      )
    }.toMap

    new CategoriesALSModels(
      modelsMap = categoriesMap,
      userStringIntMap = userStringIntMap,
      itemStringIntMap = itemStringIntMap
    )
  }

  def predict(models: CategoriesALSModels, query: Query): PredictedResult = {
    models.modelsMap.get(query.category).map { model =>
      // Convert String ID to Int index for Mllib
      models.userStringIntMap.get(query.user).map { userInt =>
        // create inverse view of itemStringIntMap
        val itemIntStringMap = models.itemStringIntMap.inverse
        // recommendProducts() returns Array[MLlibRating], which uses item Int
        // index. Convert it to String ID for returning PredictedResult
        val itemScores = model.recommendProducts(userInt, query.num)
          .map (r => ItemScore(itemIntStringMap(r.product), r.rating))
        PredictedResult(itemScores)
      }.getOrElse{
        logger.info(s"No prediction for unknown user ${query.user}.")
        PredictedResult(Array.empty)
      }
    }
    .getOrElse {
      logger.info(s"No prediction for unknown category ${query.category}.")
      PredictedResult(Array.empty)
    }
  }
}
