package io.prediction.engines.regression

import io.prediction.BaseParams
import io.prediction.api.LDataSource
import io.prediction.api.LPreparator

import io.prediction.workflow.APIDebugWorkflow

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.util.MLUtils
import io.prediction.util.Util
import scala.io.Source

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
    DataSourceParams, TrainingData, Vector[Double], Double](dsp) {
  def read(): Seq[(TrainingData, Seq[(Vector[Double], Double)])] = {
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

// When n = 0, don't drop data
// When n > 0, drop data when index mod n == k
case class PreparatorParams(n: Int = 0, k: Int = 0) extends BaseParams

class LocalPreparator(val pp: PreparatorParams)
  extends LPreparator[PreparatorParams, TrainingData, TrainingData](pp) {
  def prepare(td: TrainingData): TrainingData = {
    val xyi: Vector[(Vector[Double], Double)] = td.x.zip(td.y)
      .zipWithIndex
      .filter{ e => (e._2 % pp.n) != pp.k}
      .map{ e => (e._1._1, e._1._2) }
    TrainingData(xyi.map(_._1), xyi.map(_._2))
  }
}


object LocalAPIRunner {
  def main(args: Array[String]) {
    val filepath = "data/lr_data.txt"
    val dataSourceParams = new DataSourceParams(filepath)
    val preparatorParams = new PreparatorParams(n = 2, k = 0)
    
    APIDebugWorkflow.run(
        dataSourceClass = classOf[LocalDataSource],
        dataSourceParams = dataSourceParams,
        preparatorClass = classOf[LocalPreparator],
        preparatorParams = preparatorParams,
        batch = "Regress Man API")

  }
}

