package org.apache.spark.mllib.recommendation
// This must be the same package as Spark's MatrixFactorizationModel because
// MatrixFactorizationModel's constructor is private and we are using
// its constructor in order to save and load the model

import org.template.recommendation.AlgorithmParams

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

class PMatrixFactorizationModel(rank: Int,
    userFeatures: RDD[(Int, Array[Double])],
    productFeatures: RDD[(Int, Array[Double])])
  extends MatrixFactorizationModel(rank, userFeatures, productFeatures)
  with IPersistentModel[AlgorithmParams] {
  def save(id: String, params: AlgorithmParams, sc: SparkContext): Boolean = {
    if (params.persistModel) {
      sc.parallelize(Seq(rank)).saveAsObjectFile(s"/tmp/${id}/rank")
      userFeatures.saveAsObjectFile(s"/tmp/${id}/userFeatures")
      productFeatures.saveAsObjectFile(s"/tmp/${id}/productFeatures")
    }
    params.persistModel
  }
}

object PMatrixFactorizationModel
  extends IPersistentModelLoader[AlgorithmParams, PMatrixFactorizationModel] {
  def apply(id: String, params: AlgorithmParams, sc: Option[SparkContext]) = {
    new PMatrixFactorizationModel(
      rank = sc.get.objectFile[Int](s"/tmp/${id}/rank").first,
      userFeatures = sc.get.objectFile(s"/tmp/${id}/userFeatures"),
      productFeatures = sc.get.objectFile(s"/tmp/${id}/productFeatures"))
  }
}
