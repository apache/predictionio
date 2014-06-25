package io.prediction.engines.regression

import io.prediction.BaseParams
import io.prediction._
import io.prediction.core.BaseDataPreparator
import io.prediction.core.BaseEngine
import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseValidator
import io.prediction.core.Spark2LocalAlgorithm
import io.prediction.core.SparkDataPreparator
import io.prediction.workflow.EvaluationWorkflow
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.mllib.regression.RegressionModel
import org.apache.spark.rdd.RDD   
import org.apache.spark.mllib.util.MLUtils

// Maybe also remove this subclassing too
class EvalDataParams(val filepath: String, val k: Int, val seed: Int = 9527)
extends BaseParams

object SparkRegressionEvaluator extends EvaluatorFactory {
  def apply() = {
    new BaseEvaluator(
      classOf[DataPrep],
      classOf[Validator])
  }
}

// DataPrep
class DataPrep 
    extends BaseDataPreparator[
        EvalDataParams,
        Null,
        Null,
        RDD[LabeledPoint],
        Vector,
        Double] {
  override
  def prepare(sc: SparkContext, params: EvalDataParams)
  : Map[Int, 
      (Null, Null, RDD[LabeledPoint], RDD[(Vector, Double)])] = {
    val input = sc.textFile(params.filepath)
    val points = input.map { line =>
      val parts = line.split(' ').map(_.toDouble)
      LabeledPoint(parts(0), Vectors.dense(parts.drop(1)))
    }

    MLUtils.kFold(points, params.k, params.seed)
    .zipWithIndex
    .map { case (oneEval, ei) => {
      (ei, (null, null, oneEval._1, oneEval._2.map(p => (p.features, p.label))))
    }}
    .toMap
  }
}

// Validator
class Validator 
    extends BaseValidator[
        Null, Null, Null,
        Vector, Double, Double, 
        (Double, Double), Double, String] {

  def validate(feature: Vector, prediction: Double, actual: Double)
  : (Double, Double) = {
    (prediction, actual)
  }

  def validateSet(tdp: Null, vdp: Null, input: Seq[(Double, Double)])
  : Double = {
    val units = input.map(e => math.pow((e._1 - e._2), 2))
    units.sum / units.length
  }

  def crossValidate(input: Seq[(Null, Null, Double)]): String = {
    input.map(e => f"MSE: ${e._3}%8.6f").mkString("\n")
  }
}

class AlgoParams(val numIterations: Int = 200) extends BaseParams

// Algorithm
class Algorithm 
  extends Spark2LocalAlgorithm[
      RDD[LabeledPoint], Vector, Double, RegressionModel, AlgoParams] {
  var numIterations: Int = 0

  override def init(params: AlgoParams): Unit = {
    numIterations = params.numIterations
  }
  
  def train(data: RDD[LabeledPoint]): RegressionModel = {
    LinearRegressionWithSGD.train(data, numIterations)
  }

  def predict(model: RegressionModel, feature: Vector): Double = {
    model.predict(feature)
  }
}

object RegressionEngine extends EngineFactory {
  def apply() = {
    new Spark2LocalSimpleEngine(classOf[Algorithm])
  }
}

object Runner {
  def main(args: Array[String]) {
    val filepath = "data/lr_data.txt"
    val evalDataParams = new EvalDataParams(filepath, 3)

    val evaluator = SparkRegressionEvaluator()

    val engine = RegressionEngine()

    val algoParams = new AlgoParams(numIterations = 300)

    EvaluationWorkflow.run(
        "Regress Man", 
        evalDataParams, 
        null, 
        null, 
        Seq(("", algoParams)), 
        null,
        engine,
        evaluator)

  }
}
