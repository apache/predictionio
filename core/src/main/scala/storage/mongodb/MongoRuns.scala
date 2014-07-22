package io.prediction.storage.mongodb

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import com.github.nscala_time.time.Imports._

import io.prediction.storage.{ Run, Runs }

/** MongoDB implementation of Run. */
class MongoRuns(client: MongoClient, dbname: String) extends Runs {
  private val db = client(dbname)
  private val runColl = db("runs")
  private val seq = new MongoSequences(db)

  RegisterJodaTimeConversionHelpers()

  def insert(run: Run): String = {
    val sn = seq.genNextDaily("run")
    val now = DateTime.now
    val year = now.year.get
    val month = now.month.get
    val day = now.day.get
    val id = f"$year%04d$month%02d$day%02d$sn%04d"
    val obj = MongoDBObject(
      "_id"                    -> id,
      "startTime"              -> run.startTime,
      "endTime"                -> run.endTime,
      "engineId"               -> run.engineId,
      "engineVersion"          -> run.engineVersion,
      "engineFactory"          -> run.engineFactory,
      "metricsClass"           -> run.metricsClass,
      "batch"                  -> run.batch,
      "env"                    -> run.env,
      "dataSourceParams"       -> run.dataSourceParams,
      "preparatorParams"       -> run.preparatorParams,
      "algorithmsParams"       -> run.algorithmsParams,
      "servingParams"          -> run.servingParams,
      "metricsParams"          -> run.metricsParams,
      "models"                 -> run.models,
      "multipleMetricsResults" -> run.multipleMetricsResults)
    runColl.save(obj)
    id
  }

  def get(id: String): Option[Run] =
    runColl.findOne(MongoDBObject("_id" -> id)) map { dbObjToRun(_) }

  def update(run: Run): Unit = {
    val obj = MongoDBObject(
      "_id"                    -> run.id,
      "startTime"              -> run.startTime,
      "endTime"                -> run.endTime,
      "engineId"               -> run.engineId,
      "engineVersion"          -> run.engineVersion,
      "engineFactory"          -> run.engineFactory,
      "metricsClass"           -> run.metricsClass,
      "batch"                  -> run.batch,
      "env"                    -> run.env,
      "dataSourceParams"       -> run.dataSourceParams,
      "preparatorParams"       -> run.preparatorParams,
      "algorithmsParams"       -> run.algorithmsParams,
      "servingParams"          -> run.servingParams,
      "metricsParams"          -> run.metricsParams,
      "models"                 -> run.models,
      "multipleMetricsResults" -> run.multipleMetricsResults)
    runColl.save(obj)
  }

  def delete(id: String): Unit = runColl.remove(MongoDBObject("_id" -> id))

  private def dbObjToRun(dbObj: DBObject): Run = Run(
    id = dbObj.as[String]("_id"),
    startTime = dbObj.as[DateTime]("startTime"),
    endTime = dbObj.as[DateTime]("endTime"),
    engineId = dbObj.as[String]("engineId"),
    engineVersion = dbObj.as[String]("engineVersion"),
    engineFactory = dbObj.as[String]("engineFactory"),
    metricsClass = dbObj.as[String]("metricsClass"),
    batch = dbObj.as[String]("batch"),
    env = dbObj.as[Map[String, String]]("env"),
    dataSourceParams = dbObj.as[String]("dataSourceParams"),
    preparatorParams = dbObj.as[String]("preparatorParams"),
    algorithmsParams = dbObj.as[String]("algorithmsParams"),
    servingParams = dbObj.as[String]("servingParams"),
    metricsParams = dbObj.as[String]("metricsParams"),
    models = dbObj.as[Array[Byte]]("models"),
    multipleMetricsResults = dbObj.as[String]("multipleMetricsResults"))

  class MongoRunIterator(it: MongoCursor) extends Iterator[Run] {
    def next = dbObjToRun(it.next)
    def hasNext = it.hasNext
  }
}
