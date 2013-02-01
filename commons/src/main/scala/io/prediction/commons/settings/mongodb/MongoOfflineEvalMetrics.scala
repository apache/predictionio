package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{OfflineEvalMetric, OfflineEvalMetrics}

import com.mongodb.casbah.Imports._

class MongoOfflineEvalMetrics(db: MongoDB) extends OfflineEvalMetrics {

  private val emptyObj = MongoDBObject()
  private val offlineEvalMetricsColl = db("offlineEvalMetrics")
  private val seq = new MongoSequences(db)
  private def genNextId = seq.genNext("offlineEvalMetricid")

  private val getFields = MongoDBObject( // fields to be read
    "name" -> 1,
    "metrictype" -> 1,
    "jarname" -> 1,
    "evalid" -> 1,
    "params" -> 1
  )

  class MongoOfflineEvalMetricIterator(it: MongoCursor) extends Iterator[OfflineEvalMetric] {
    def next = dbObjToOfflineEvalMetric(it.next)
    def hasNext = it.hasNext
  }

  /** create OfflineEvalMetric object from DBObject */
  private def dbObjToOfflineEvalMetric(dbObj: DBObject) = {
    OfflineEvalMetric(
      id = dbObj.as[Int]("_id"),
      name = dbObj.as[String]("name"),
      metrictype = dbObj.as[String]("metrictype"),
      jarname = dbObj.as[String]("jarname"),
      evalid = dbObj.as[Int]("evalid"),
      params = MongoUtils.dbObjToMap(dbObj.as[DBObject]("params"))
    )
  }

  /** Insert a metric and return id */
  def insert(metric: OfflineEvalMetric): Int = {
    val id = genNextId

    offlineEvalMetricsColl.insert(MongoDBObject(
      "_id" -> id,
      "name" -> metric.name,
      "metrictype" -> metric.metrictype,
      "jarname" -> metric.jarname,
      "evalid" -> metric.evalid,
      "params" -> metric.params
    ))

    id
  }

  /** Get a metric by its ID */
  def get(id: Int): Option[OfflineEvalMetric] = {
    offlineEvalMetricsColl.findOne(MongoDBObject("_id" -> id), getFields) map { dbObjToOfflineEvalMetric(_) }
  }

  /** Get metrics by OfflineEval id */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalMetric] = new MongoOfflineEvalMetricIterator(
    offlineEvalMetricsColl.find(MongoDBObject("evalid" -> evalid), getFields).sort(MongoDBObject("name" -> 1))
  )

  /** Update metric */
  def update(metric: OfflineEvalMetric) = {
    offlineEvalMetricsColl.update(MongoDBObject("_id" -> metric.id), MongoDBObject(
      "name" -> metric.name,
      "metrictype" -> metric.metrictype,
      "jarname" -> metric.jarname,
      "evalid" -> metric.evalid,
      "params" -> metric.params
    ))
  }

  /** Delete metric by its ID */
  def delete(id: Int) = {
    offlineEvalMetricsColl.remove(MongoDBObject("_id" -> id))
  }
}
