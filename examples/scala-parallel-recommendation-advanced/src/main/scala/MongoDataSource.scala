package org.examples.recommendation

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

import org.apache.hadoop.conf.Configuration
import org.bson.BSONObject
import com.mongodb.hadoop.MongoInputFormat

case class MongoDataSourceParams(
  val host: String,
  val port: Int,
  val db: String, // DB name
  val collection: String // collection name
) extends Params

class MongoDataSource(val dsp: MongoDataSourceParams)
  extends PDataSource[MongoDataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActualResult] {

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val config = new Configuration()
    config.set("mongo.input.uri",
      s"mongodb://${dsp.host}:${dsp.port}/${dsp.db}.${dsp.collection}")

    val mongoRDD = sc.newAPIHadoopRDD(config,
      classOf[MongoInputFormat],
      classOf[Object],
      classOf[BSONObject])

    // mongoRDD contains tuples of (ObjectId, BSONObject)
    val ratings = mongoRDD.map { case (id, bson) =>
      Rating(bson.get("uid").asInstanceOf[Int],
        bson.get("iid").asInstanceOf[Int],
        bson.get("rating").asInstanceOf[Double])
    }
    new TrainingData(ratings)
  }
}

object MongoDataSourceTest {
  def main(args: Array[String]) {
    val dsp = MongoDataSourceParams(
      host = "127.0.0.1",
      port = 27017,
      db = "test",
      collection = "sample_ratings")

    Workflow.run(
      dataSourceClassOpt = Some(classOf[MongoDataSource]),
      dataSourceParams = dsp,
      params = WorkflowParams(
        batch = "Template: Recommendations",
        verbose = 3
      )
    )
  }
}
