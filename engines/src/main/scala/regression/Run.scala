package io.prediction.engines.regression

import io.prediction.BaseParams
import io.prediction._
import io.prediction.core.BaseDataPreparator
import io.prediction.core.BaseEngine
import io.prediction.core.BaseEvaluator
import io.prediction.core.Spark2LocalAlgorithm
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

// Maybe also remove this subclassing too
class EvalDataParams(val filepath: String, val k: Int, val seed: Int = 9527)
extends BaseParams

object SparkRegressionEvaluator extends EvaluatorFactory {
  def apply() = {
    new BaseEvaluator(
      classOf[DataPrep],
      //classOf[RegressionValidator])
      classOf[MeanSquareErrorValidator[Vector]])
  }
}

// DataPrep
class DataPrep
    extends BaseDataPreparator[
        EvalDataParams,
        EmptyParams,
        EmptyParams,
        RDD[LabeledPoint],
        Vector,
        Double] {
  override
  def prepareBase(sc: SparkContext, baseParams: BaseParams)
  : Map[Int,
      (EmptyParams, EmptyParams, RDD[LabeledPoint], RDD[(Vector, Double)])] = {
    val params = baseParams.asInstanceOf[EvalDataParams]

    val input = sc.textFile(params.filepath)
    val points = input.map { line =>
      val parts = line.split(' ').map(_.toDouble)
      LabeledPoint(parts(0), Vectors.dense(parts.drop(1)))
    }

    MLUtils.kFold(points, params.k, params.seed)
    .zipWithIndex
    .map { case (oneEval, ei) => {
      (ei, (EmptyParams(), EmptyParams(),
        oneEval._1, oneEval._2.map(p => (p.features, p.label))))
    }}
    .toMap
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

class VectorSerializer extends CustomSerializer[Vector](format => (
  {
    case JArray(x) =>
      val v = x.toArray.map { y =>
        y match {
          case JDouble(z) => z
        }
      }
      new DenseVector(v)
  },
  {
    case x: Vector =>
      JArray(x.toArray.toList.map(d => JDouble(d)))
  }
))

object RegressionEngine extends EngineFactory {
  def apply() = {
    new Spark2LocalSimpleEngine(
      classOf[Algorithm]) {
      override val formats = Util.json4sDefaultFormats + new VectorSerializer
    }
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
