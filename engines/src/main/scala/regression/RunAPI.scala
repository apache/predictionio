package io.prediction.engines.regression

import io.prediction.BaseParams
import io.prediction.EmptyParams
import io.prediction.api.LDataSource
import io.prediction.api.LPreparator
import io.prediction.api.LAlgorithm
import io.prediction.api.LServing
import io.prediction.api.Metrics

import io.prediction.workflow.APIDebugWorkflow

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.util.MLUtils
import io.prediction.util.Util
import scala.io.Source

import breeze.linalg.DenseMatrix
import breeze.linalg.DenseVector
import breeze.linalg.inv
import nak.regress.LinearRegression

import org.json4s._

case class DataSourceParams(val filepath: String, val seed: Int = 9527)
extends BaseParams

case class TrainingData(x: Vector[Vector[Double]], y: Vector[Double])
extends Serializable {
  val r = x.length
  val c = x.head.length
}

class LocalDataSource(val dsp: DataSourceParams)
extends LDataSource[
    DataSourceParams, Null, TrainingData, Vector[Double], Double] {
  def read(): Seq[(Null, TrainingData, Seq[(Vector[Double], Double)])] = {
    val lines = Source.fromFile(dsp.filepath).getLines
      .toSeq.map(_.split(" ", 2))

    // FIXME: Use different training / testing data.
    val x = lines.map{ _(1).split(' ').map{_.toDouble} }.map{ e => Vector(e:_*)}
    val y = lines.map{ _(0).toDouble }

    val td = TrainingData(Vector(x:_*), Vector(y:_*))

    val oneData = (null, td, x.zip(y))
    return Seq(oneData)
  }
}

// When n = 0, don't drop data
// When n > 0, drop data when index mod n == k
case class PreparatorParams(n: Int = 0, k: Int = 0) extends BaseParams

class LocalPreparator(val pp: PreparatorParams = PreparatorParams())
  extends LPreparator[PreparatorParams, TrainingData, TrainingData] {
  def prepare(td: TrainingData): TrainingData = {
    val xyi: Vector[(Vector[Double], Double)] = td.x.zip(td.y)
      .zipWithIndex
      .filter{ e => (e._2 % pp.n) != pp.k}
      .map{ e => (e._1._1, e._1._2) }
    TrainingData(xyi.map(_._1), xyi.map(_._2))
  }
}

class LocalAlgorithm
  extends LAlgorithm[
      EmptyParams, TrainingData, Array[Double], Vector[Double], Double] {

  def train(td: TrainingData): Array[Double] = {
    val xArray: Array[Double] = td.x.foldLeft(Vector[Double]())(_ ++ _).toArray
    // DenseMatrix.create fills first column, then second.
    val m = DenseMatrix.create[Double](td.c, td.r, xArray).t
    val y = DenseVector[Double](td.y.toArray)
    val result = LinearRegression.regress(m, y)
    return result.data.toArray
  }

  def predict(model: Array[Double], query: Vector[Double]) = {
    model.zip(query).map(e => e._1 * e._2).sum
  }
}

class Serving extends LServing[EmptyParams, Vector[Double], Double] {
  def serve(query: Vector[Double], predictions: Seq[Double]): Double = {
    return predictions.head
  }
}

class MeanSquareError extends Metrics[EmptyParams, Null, 
    Vector[Double], Double, Double, 
    (Double, Double), Seq[(Double, Double)], String] {
  def computeUnit(q: Vector[Double], p: Double, a: Double)
  : (Double, Double) = (p, a)

  def computeSet(ep: Null, s: Seq[(Double, Double)]): Seq[(Double, Double)] = s

  def computeMultipleSets(input: Seq[(Null, Seq[(Double, Double)])]): String = {
    val data: Seq[(Double, Double)] = input.map(_._2).flatten
    val units = data.map(e => math.pow(e._1 - e._2, 2))
    val mse = units.sum / units.length
    f"MSE: ${mse}%8.6f"
  }
}


object LocalAPIRunner {
  def main(args: Array[String]) {
    val filepath = "data/lr_data.txt"
    val dataSourceParams = new DataSourceParams(filepath)
    val preparatorParams = new PreparatorParams(n = 2, k = 0)
    val emptyParams = EmptyParams()
   
    APIDebugWorkflow.run(
        dataSourceClass = classOf[LocalDataSource],
        dataSourceParams = dataSourceParams,
        preparatorClass = classOf[LocalPreparator],
        preparatorParams = preparatorParams,
        algorithmClassMap = Map(
          "" -> classOf[LocalAlgorithm]),
        algorithmParamsList = Seq(
          ("", emptyParams)),
        servingClass = classOf[Serving],
        servingParams = emptyParams,
        metricsClass = classOf[MeanSquareError],
        metricsParams = emptyParams,
        batch = "Regress Man API")

  }
}

