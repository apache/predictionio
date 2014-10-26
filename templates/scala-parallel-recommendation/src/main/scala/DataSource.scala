package org.template.recommendation

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

case class DataSourceParams(val filepath: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[DataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActual] {

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val data = sc.textFile(dsp.filepath)
    val ratings: RDD[Rating] = data.map(_.split("::") match {
      case Array(user, item, rate) =>
        Rating(user.toInt, item.toInt, rate.toDouble)
    })
    new TrainingData(ratings)
  }
}

class TrainingData(
  val ratings: RDD[Rating]
) extends Serializable
