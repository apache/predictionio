package org.template.classification

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.mllib.linalg.Vectors

case class AlgorithmParams(
  val lambda: Double
) extends Params

class Query(
  val features: Array[Double]
) extends Serializable

class Prediction(
  val label: Double
) extends Serializable


// extends P2LAlgorithm because the MLlib's NaiveBayesModel doesn't contain RDD.
class NaiveBayesAlgorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[AlgorithmParams, PreparedData,
      NaiveBayesModel, Query, Prediction] {

  def train(data: PreparedData): NaiveBayesModel = {
    NaiveBayes.train(data.labeledPoints, ap.lambda)
  }

  def predict(model: NaiveBayesModel, query: Query): Prediction = {
    val label = model.predict(Vectors.dense(query.features))
    new Prediction(label)
  }

}
