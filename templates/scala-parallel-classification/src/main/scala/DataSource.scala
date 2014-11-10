package org.template.classification

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors

case class DataSourceParams(val filepath: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[DataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActualResult] {

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val data = sc.textFile(dsp.filepath)
    val labeledPoints: RDD[LabeledPoint] = data.map { line =>
      val parts = line.split(',')
      LabeledPoint(parts(0).toDouble,
        Vectors.dense(parts(1).split(' ').map(_.toDouble)))
    }

    new TrainingData(labeledPoints)
  }
}

class TrainingData(
  val labeledPoints: RDD[LabeledPoint]
) extends Serializable
