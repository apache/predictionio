package io.prediction.engines.regression

import io.prediction.BaseParams
import io.prediction.api.LDataSource

import io.prediction.workflow.APIDebugWorkflow

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.util.MLUtils
import io.prediction.util.Util
import scala.io.Source

import org.json4s._

class DataSourceParams(val filepath: String, val seed: Int = 9527)
extends BaseParams

case class TrainingData(x: Vector[Vector[Double]], y: Vector[Double]) {
  val r = x.length
  val c = x.head.length
}

class LocalDataSource(val dsp: DataSourceParams)
extends LDataSource[
    DataSourceParams, TrainingData, Vector[Double], Double](dsp) {
  def prepare(): Seq[(TrainingData, Seq[(Vector[Double], Double)])] = {
    val lines = Source.fromFile(dsp.filepath).getLines
      .toSeq.map(_.split(" ", 2))

    // FIXME: Use different training / testing data.
    val x = lines.map{ _(1).split(' ').map{_.toDouble} }.map{ e => Vector(e:_*)}
    val y = lines.map{ _(0).toDouble }

    val td = TrainingData(Vector(x:_*), Vector(y:_*))

    val oneData = (td, x.zip(y))
    return Seq(oneData)
  }
}

object LocalAPIRunner {
  def main(args: Array[String]) {
    val filepath = "data/lr_data.txt"
    val dataSourceParams = new DataSourceParams(filepath)
    
    APIDebugWorkflow.run(
        dataSourceClass = classOf[LocalDataSource],
        dataSourceParams = dataSourceParams,
        batch = "Regress Man API")

  }
}

