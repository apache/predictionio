package io.prediction.storage

import io.prediction.{
  BaseEvaluationDataParams,
  BaseValidationParams,
  BaseCleanserParams,
  BaseAlgoParams,
  BaseServerParams,
  BaseCrossValidationResults
}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

object MongoRuns {
  RegisterJodaTimeConversionHelpers()

  def apply(db: MongoDB): MongoRuns = new MongoRuns(db)
}

/** MongoDB implementation of Run. */
class MongoRuns(db: MongoDB) extends Runs {
  private val runColl = db("runs")
  private val seq = new MongoSequences(db)
  implicit private val formats = Serialization.formats(NoTypeHints)

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
      "evaluationDataParams"   -> write(run.evaluationDataParams),
      "validationParams"       -> write(run.validationParams),
      "cleanserParams"         -> write(run.cleanserParams),
      "algoParamsList"         -> write(run.algoParamsList),
      "serverParams"           -> write(run.serverParams),
      "crossValidationResults" -> write(run.crossValidationResults))
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
    evaluationDataParams = read[BaseEvaluationDataParams](
      dbObj.as[String]("evaluationDataParams")),
    validationParams = read[BaseValidationParams](
      dbObj.as[String]("validationParams")),
    cleanserParams = read[BaseCleanserParams](
      dbObj.as[String]("cleanserParams")),
    algoParamsList = read[Seq[(String, BaseAlgoParams)]](
      dbObj.as[String]("algoParamsList")),
    serverParams = read[BaseServerParams](dbObj.as[String]("serverParams")),
    crossValidationResults = read[BaseCrossValidationResults](
      dbObj.as[String]("crossValidationResults")))

  class MongoRunIterator(it: MongoCursor) extends Iterator[Run] {
    def next = dbObjToRun(it.next)
    def hasNext = it.hasNext
  }
}
