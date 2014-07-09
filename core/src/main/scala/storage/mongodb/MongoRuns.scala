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
      "engineManifestId"       -> run.engineManifestId,
      "engineManifestVersion"  -> run.engineManifestVersion,
      "batch"                  -> run.batch,
      "evaluationDataParams"   -> run.evaluationDataParams,
      "validationParams"       -> run.validationParams,
      "cleanserParams"         -> run.cleanserParams,
      "algoParamsList"         -> run.algoParamsList,
      "serverParams"           -> run.serverParams,
      "models"                 -> run.models,
      "crossValidationResults" -> run.crossValidationResults)
    runColl.save(obj)
    id
  }

  def get(id: String): Option[Run] =
    runColl.findOne(MongoDBObject("_id" -> id)) map { dbObjToRun(_) }

  def delete(id: String): Unit = runColl.remove(MongoDBObject("_id" -> id))

  private def dbObjToRun(dbObj: DBObject): Run = Run(
    id = dbObj.as[String]("_id"),
    startTime = dbObj.as[DateTime]("startTime"),
    endTime = dbObj.as[DateTime]("endTime"),
    engineManifestId = dbObj.as[String]("engineManifestId"),
    engineManifestVersion = dbObj.as[String]("engineManifestVersion"),
    batch = dbObj.as[String]("batch"),
    evaluationDataParams = dbObj.as[String]("evaluationDataParams"),
    validationParams = dbObj.as[String]("validationParams"),
    cleanserParams = dbObj.as[String]("cleanserParams"),
    algoParamsList = dbObj.as[String]("algoParamsList"),
    serverParams = dbObj.as[String]("serverParams"),
    models = dbObj.as[Array[Byte]]("models"),
    crossValidationResults = dbObj.as[String]("crossValidationResults"))

  class MongoRunIterator(it: MongoCursor) extends Iterator[Run] {
    def next = dbObjToRun(it.next)
    def hasNext = it.hasNext
  }
}
