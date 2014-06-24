package io.prediction.engines.regression

import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Vector

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD   

import io.prediction.BaseEvaluationDataParams
import io.prediction.core.BaseDataPreparator
import io.prediction.core.BaseValidator
import io.prediction.core.SparkDataPreparator
import io.prediction._
//import io.prediction.core.SparkEvaluator
import io.prediction.core.BaseEvaluator
import io.prediction.core.Spark2LocalAlgorithm
import org.apache.spark.mllib.regression.RegressionModel
import io.prediction.core.BaseEngine
import io.prediction.workflow.EvaluationWorkflow

class SparkNoOptCleanser extends SparkDefaultCleanser[RDD[LabeledPoint]] {}

// Maybe also remove this subclassing too
class EvalDataParams(val filepath: String, val k: Int)
extends BaseEvaluationDataParams

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
      val parts = line.split(' ')
      LabeledPoint(
        parts(0).toDouble, 
        Vectors.dense(parts.drop(1).map(_.toDouble)))
    }

    (0 until params.k).map { case ei => {
      (ei, (null, null, points, points.map(p => (p.features, p.label))))
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
    input.map(e => s"MSE: ${e._3}").mkString("\n")
  }
}

// Algorithm
class Algorithm 
  extends Spark2LocalAlgorithm[
      RDD[LabeledPoint], 
      Vector,
      Double,
      RegressionModel,
      Null] {
  
  def train(data: RDD[LabeledPoint]): RegressionModel = {
    val numIterations = 50
    LinearRegressionWithSGD.train(data, numIterations)
  }

  def predict(model: RegressionModel, feature: Vector): Double = {
    model.predict(feature)
  }
}



object Runner {
  def main(args: Array[String]) {
    val filepath = "data/lr_data.txt"
    val evalDataParams = new EvalDataParams(filepath, 2)

    val evaluator = SparkRegressionEvaluator()

    val engine = new BaseEngine(
      classOf[SparkNoOptCleanser],
      Map("algo" -> classOf[Algorithm]),
      classOf[DefaultServer[Vector, Double]])

    EvaluationWorkflow.run(
        "Regress Man", 
        evalDataParams, 
        null, 
        null, 
        Seq(("algo", null)), 
        null,
        engine,
        evaluator)

  }
}
