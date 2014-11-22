package org.apache.spark.mllib.recommendation
// This must be the same package as Spark's MatrixFactorizationModel because
// MatrixFactorizationModel's constructor is private and we are using
// its constructor in order to save and load the model

import org.template.recommendation.ALSAlgorithmParams

import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

class PersistentMatrixFactorizationModel(rank: Int,
    userFeatures: RDD[(Int, Array[Double])],
    productFeatures: RDD[(Int, Array[Double])])
  extends MatrixFactorizationModel(rank, userFeatures, productFeatures)
  with IPersistentModel[ALSAlgorithmParams] {
  def save(id: String, params: ALSAlgorithmParams,
    sc: SparkContext): Boolean = {

    sc.parallelize(Seq(rank)).saveAsObjectFile(s"/tmp/${id}/rank")
    userFeatures.saveAsObjectFile(s"/tmp/${id}/userFeatures")
    productFeatures.saveAsObjectFile(s"/tmp/${id}/productFeatures")
    true
  }
}

object PersistentMatrixFactorizationModel
  extends IPersistentModelLoader[ALSAlgorithmParams,
    PersistentMatrixFactorizationModel] {
  def apply(id: String, params: ALSAlgorithmParams,
    sc: Option[SparkContext]) = {
    new PersistentMatrixFactorizationModel(
      rank = sc.get.objectFile[Int](s"/tmp/${id}/rank").first,
      userFeatures = sc.get.objectFile(s"/tmp/${id}/userFeatures"),
      productFeatures = sc.get.objectFile(s"/tmp/${id}/productFeatures"))
  }
}
