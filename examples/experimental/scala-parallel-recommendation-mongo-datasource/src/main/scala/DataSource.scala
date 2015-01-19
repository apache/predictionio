package org.template.recommendation

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

import org.apache.hadoop.conf.Configuration // ADDED
import org.bson.BSONObject // ADDED
import com.mongodb.hadoop.MongoInputFormat // ADDED

case class DataSourceParams( // CHANGED
  host: String,
  port: Int,
  db: String, // DB name
  collection: String // collection name
) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    // CHANGED
    val config = new Configuration()
    config.set("mongo.input.uri",
      s"mongodb://${dsp.host}:${dsp.port}/${dsp.db}.${dsp.collection}")

    val mongoRDD = sc.newAPIHadoopRDD(config,
      classOf[MongoInputFormat],
      classOf[Object],
      classOf[BSONObject])

    // mongoRDD contains tuples of (ObjectId, BSONObject)
    val ratings = mongoRDD.map { case (id, bson) =>
      Rating(bson.get("uid").asInstanceOf[String],
        bson.get("iid").asInstanceOf[String],
        bson.get("rating").asInstanceOf[Double])
    }
    new TrainingData(ratings)
  }
}

case class Rating(
  user: String,
  item: String,
  rating: Double
)

class TrainingData(
  val ratings: RDD[Rating]
) extends Serializable {
  override def toString = {
    s"ratings: [${ratings.count()}] (${ratings.take(2).toList}...)"
  }
}
