package io.prediction.engines.recommendations

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
      classOf[RecommendationValidator])
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

class RecommendationValidator 
extends Validator[
    EmptyParams, EmptyParams, EmptyParams,
    (Int, Int), Double, Double,
    (Double, Double), Double, String] {
  def validate(feature: (Int, Int), prediction: Double, actual: Double)
  : (Double, Double) = (prediction, actual)

  def validateSet(tdp: EmptyParams, vdp: EmptyParams,
    input: Seq[(Double, Double)])
  : Double = {
    val units = input.map(e => math.pow((e._1 - e._2), 2))
    units.sum / units.length
  }

  def crossValidate(input: Seq[(EmptyParams, EmptyParams, Double)]): String = {
    input.map(e => f"MSE: ${e._3}%8.6f").mkString("\n")
  }
}

// Algorithms
class AlgoParams(
  val rank: Int = 10, 
  val numIterations: Int = 20,
  val lambda: Double = 0.01) extends BaseParams

object RecommendationsEngine extends EngineFactory {
  def apply() = {
    new ParallelSimpleEngine(classOf[Algorithm])
  }
}

class Algorithm
  extends ParallelAlgorithm[
      RDD[Rating], (Int, Int), Double, MatrixFactorizationModel, AlgoParams] {
  var rank: Int = 0
  var numIterations: Int = 0
  var lambda: Double = 0.0

  override def init(params: AlgoParams): Unit = {
    rank = params.rank
    numIterations = params.numIterations
    lambda = params.lambda
  }

  def train(data: RDD[Rating]): MatrixFactorizationModel = {
    ALS.train(data, rank, numIterations, lambda)
  }

  def batchPredict(
    model: MatrixFactorizationModel, 
    feature: RDD[(Long, (Int, Int))])
  : RDD[(Long, Double)] = {
    val indexlessFeature = feature.values

    val prediction: RDD[Rating] = model.predict(indexlessFeature)

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
        "RecommMan",
        evalDataParams,
        null,
        null,
        Seq(("", algoParams)),
        null,
        engine,
        evaluator)

  }
}

object Good {
  def main(args: Array[String]) {
    val batch = "ALS" 
    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    // Load and parse the data
    //val data = sc.textFile("mllib/data/als/test.data")
    val data = sc.textFile("data/movielens.txt")
    val ratings = data.map(_.split("::") match { case Array(user, item, rate) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
    })
  
    // Build the recommendation model using ALS
    val rank = 10
    val numIterations = 20
    val model = ALS.train(ratings, rank, numIterations, 0.01)
  
    // Evaluate the model on rating data
    val usersProducts = ratings.map { case Rating(user, product, rate) =>
      (user, product)
    }
    val predictions = 
      model.predict(usersProducts).map { case Rating(user, product, rate) => 
      ((user, product), rate)
    }
    val ratesAndPreds = ratings.map { case Rating(user, product, rate) => 
      ((user, product), rate)
    }.join(predictions)
  
    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) => 
      val err = (r1 - r2)
      err * err
    }.mean()
    println("Mean Squared Error = " + MSE)

  }
}


