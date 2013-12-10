package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ OfflineEvalMetricInfo, OfflineEvalMetricInfos }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of OfflineEvalMetricInfos. */
class MongoOfflineEvalMetricInfos(db: MongoDB) extends OfflineEvalMetricInfos {
  private val coll = db("offlineEvalMetricInfos")

  private def dbObjToOfflineEvalMetricInfo(dbObj: DBObject) = {

    OfflineEvalMetricInfo(
      id = dbObj.as[String]("_id"),
      name = dbObj.as[String]("name"),
      description = dbObj.getAs[String]("description"),
      engineinfoids = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("engineinfoids")),
      commands = dbObj.getAs[MongoDBList]("commands") map { MongoUtils.mongoDbListToListOfString(_) },
      params = (dbObj.as[DBObject]("params") map { p => (p._1, MongoParam.dbObjToParam(p._1, p._2.asInstanceOf[DBObject])) }).toMap,
      paramsections = dbObj.as[Seq[DBObject]]("paramsections") map { MongoParam.dbObjToParamSection(_) },
      paramorder = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("paramorder")))
  }

  def insert(offlineEvalMetricInfo: OfflineEvalMetricInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id" -> offlineEvalMetricInfo.id,
      "name" -> offlineEvalMetricInfo.name,
      "engineinfoids" -> offlineEvalMetricInfo.engineinfoids,
      "params" -> offlineEvalMetricInfo.params.mapValues { MongoParam.paramToDBObj(_) },
      "paramsections" -> offlineEvalMetricInfo.paramsections.map { MongoParam.paramSectionToDBObj(_) },
      "paramorder" -> offlineEvalMetricInfo.paramorder)
    // optional fields
    val descriptionObj = offlineEvalMetricInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = offlineEvalMetricInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ descriptionObj ++ commandsObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToOfflineEvalMetricInfo(_) }

  def getAll() = coll.find().toSeq map { dbObjToOfflineEvalMetricInfo(_) }

  def getByEngineinfoid(engineinfoid: String): Seq[OfflineEvalMetricInfo] = {
    coll.find(MongoDBObject("engineinfoids" -> MongoDBObject("$in" -> Seq(engineinfoid)))).toSeq map { dbObjToOfflineEvalMetricInfo(_) }
  }

  def update(offlineEvalMetricInfo: OfflineEvalMetricInfo, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> offlineEvalMetricInfo.id)
    val requiredObj = MongoDBObject(
      "name" -> offlineEvalMetricInfo.name,
      "engineinfoids" -> offlineEvalMetricInfo.engineinfoids,
      "params" -> offlineEvalMetricInfo.params.mapValues { MongoParam.paramToDBObj(_) },
      "paramsections" -> offlineEvalMetricInfo.paramsections.map { MongoParam.paramSectionToDBObj(_) },
      "paramorder" -> offlineEvalMetricInfo.paramorder)

    val descriptionObj = offlineEvalMetricInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = offlineEvalMetricInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj ++ commandsObj, upsert)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
