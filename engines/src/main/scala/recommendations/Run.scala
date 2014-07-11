package org.apache.spark.mllib.recommendation.engine

import io.prediction.BaseParams
import io.prediction._
import io.prediction.core.BaseDataPreparator
import io.prediction.core.BaseEngine
import io.prediction.core.BaseEvaluator
import io.prediction.core.Spark2LocalAlgorithm
import io.prediction.core.ParallelAlgorithm
import io.prediction.core.SparkDataPreparator
import io.prediction.workflow.EvaluationWorkflow
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.mllib.regression.RegressionModel
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.util.MLUtils

import org.json4s._

import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel

object RecommendationsEvaluator extends EvaluatorFactory {
  def apply() = {
    new BaseEvaluator(
      classOf[DataPrep],
      classOf[MeanSquareErrorValidator[(Int, Int)]])
  }
}

class EvalDataParams(val filepath: String)
extends BaseParams

// Training Data is RDD[Rating], Rating == user::product::rate
// Feture is (user, product)
// Target is Double
class DataPrep
  extends SimpleParallelDataPreparator[
      EvalDataParams, RDD[Rating], (Int, Int), Double] {
  override
  def prepare(sc: SparkContext, params: EvalDataParams)
  : (RDD[Rating], RDD[((Int, Int), Double)]) = {

    val data = sc.textFile(params.filepath)
    val ratings = data.map(_.split("::") match {
      case Array(user, item, rate) =>
        Rating(user.toInt, item.toInt, rate.toDouble)
    })

    val featureTargets = ratings.map {
      case Rating(user, product, rate) => ((user, product), rate)
    }

    (ratings, featureTargets)
  }
}


// Algorithms
class AlgoParams(
  val rank: Int = 10,
  val numIterations: Int = 20,
  val lambda: Double = 0.01) extends BaseParams

class FeatureSerializer extends CustomSerializer[(Int, Int)](format => (
  {
    case JArray(List(JInt(x), JInt(y))) => (x.intValue, y.intValue)
  },
  {
    case x: (Int, Int) => JArray(List(JInt(x._1), JInt(x._2)))
  }
))

object RecommendationsEngine extends EngineFactory {
  def apply() = {
    new ParallelSimpleEngine(
      classOf[Algorithm],
      DefaultFormats + new FeatureSerializer)
  }
}

class PersistentMatrixFactorizationModel(m: MatrixFactorizationModel)
    extends PersistentParallelModel {

  @transient var model = m
  val rank: Int = m.rank

  def save(id: String): Unit = {
    model.productFeatures.saveAsObjectFile("/tmp/productFeatures")
    model.userFeatures.saveAsObjectFile("/tmp/userFeatures")
  }

  def load(sc: SparkContext, id: String): Unit = {
    model = new MatrixFactorizationModel(
      rank,
      sc.objectFile("/tmp/userFeatures"),
      sc.objectFile("/tmp/productFeatures"))
  }
}

class Algorithm
  extends ParallelAlgorithm[
      RDD[Rating], (Int, Int), Double, PersistentMatrixFactorizationModel, AlgoParams] {
  var _rank: Int = 0
  var _numIterations: Int = 0
  var _lambda: Double = 0.0

  override def init(params: AlgoParams): Unit = {
    _rank = params.rank
    _numIterations = params.numIterations
    _lambda = params.lambda
  }

  def train(data: RDD[Rating]): PersistentMatrixFactorizationModel = {
    new PersistentMatrixFactorizationModel(ALS.train(data, _rank, _numIterations, _lambda))
  }

  def predict(
    model: PersistentMatrixFactorizationModel,
    feature: (Int, Int)): Double = {
    model.model.predict(feature._1, feature._2)
  }

  def batchPredict(
    model: PersistentMatrixFactorizationModel,
    feature: RDD[(Long, (Int, Int))])
  : RDD[(Long, Double)] = {
    val indexlessFeature = feature.values

    val prediction: RDD[Rating] = model.model.predict(indexlessFeature)

    val p: RDD[((Int, Int), Double)] = prediction.map {
      r => ((r.user, r.product), r.rating)
    }

    feature.map{ _.swap }
    .join(p)
    .map { case (up, (fi, r)) => (fi,r) }
  }
}


object Run {
  def main(args: Array[String]) {
    val filepath = "data/movielens.txt"

    val evalDataParams = new EvalDataParams(filepath)

    val evaluator = RecommendationsEvaluator()


    val engine = RecommendationsEngine()

    val algoParams = new AlgoParams(numIterations = 10)

    EvaluationWorkflow.run(
        engine, evaluator,
        batch = "RecommMan",
        evalDataParams = evalDataParams,
        algoParams = algoParams)
  }
}
