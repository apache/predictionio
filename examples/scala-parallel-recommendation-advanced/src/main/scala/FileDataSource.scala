package org.examples.recommendation

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyDataParams
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.controller.Workflow
import io.prediction.controller.WorkflowParams
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

case class FileDataSourceParams(val filepath: String) extends Params

class FileDataSource(val dsp: FileDataSourceParams)
  extends PDataSource[FileDataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActualResult] {

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

object FileDataSourceTest {
  def main(args: Array[String]) {
    val dsp = FileDataSourceParams("data/sample_movielens_data.txt")

    Workflow.run(
      dataSourceClassOpt = Some(classOf[FileDataSource]),
      dataSourceParams = dsp,
      params = WorkflowParams(
        batch = "Template: Recommendations",
        verbose = 3
      )
    )
  }
}
