package io.prediction.commons.settings.mongodb

import io.prediction.commons.settings.{OfflineEvalResult, OfflineEvalResults}
import com.mongodb.casbah.Imports._

class MongoOfflineEvalResults(db: MongoDB) extends OfflineEvalResults {

  private val emptyObj = MongoDBObject()
  private val offlineEvalResultsColl = db("offlineEvalResults")
  private val getFields = MongoDBObject( // fields to be read
    "evalid" -> 1,
    "metricid" -> 1,
    "algoid" -> 1,
    "score" -> 1,
    "iteration" -> 1
  )

  private def dbObjToOfflineEvalResult(dbObj: DBObject) = {
    OfflineEvalResult(
      id = dbObj.as[String]("_id"),
      evalid = dbObj.as[Int]("evalid"),
      metricid = dbObj.as[Int]("metricid"),
      algoid = dbObj.as[Int]("algoid"),
      score = dbObj.as[Double]("score"),
      iteration = dbObj.as[Int]("iteration")
    )
  }

  class MongoOfflineEvalResultIterator(it: MongoCursor) extends Iterator[OfflineEvalResult] {
    def next = dbObjToOfflineEvalResult(it.next)
    def hasNext = it.hasNext
  }

  /** save(update existing or create a new one) a OfflineEvalResult and return id*/
  def save(result: OfflineEvalResult): String = {
    val id = (result.evalid + "_" + result.metricid + "_" + result.algoid + "_" + result.iteration)
    offlineEvalResultsColl.save(MongoDBObject(
      "_id" -> id,
      "evalid" -> result.evalid,
      "metricid" -> result.metricid,
      "algoid" -> result.algoid,
      "score" -> result.score,
      "iteration" -> result.iteration
    ))

    id
  }

  def getByEvalidAndMetricidAndAlgoid(evalid: Int, metricid: Int, algoid: Int): Iterator[OfflineEvalResult] = new MongoOfflineEvalResultIterator(
    offlineEvalResultsColl.find(MongoDBObject("evalid " -> evalid, "metricid" -> metricid, "algoid" -> algoid), getFields)
  )

  /** get results by OfflineEval ID */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalResult] = new MongoOfflineEvalResultIterator(
    offlineEvalResultsColl.find(MongoDBObject("evalid" -> evalid), getFields)
  )

  /** delete all results with this OfflineEval ID */
  def deleteByEvalid(evalid: Int) = {
    offlineEvalResultsColl.remove(MongoDBObject("evalid" -> evalid))
  }
}
