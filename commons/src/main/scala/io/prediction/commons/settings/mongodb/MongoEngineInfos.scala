package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{EngineInfo, EngineInfos}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of EngineInfos. */
class MongoEngineInfos(db: MongoDB) extends EngineInfos {
  private val coll = db("engineInfos")

  private def dbObjToEngineInfo(dbObj: DBObject) = EngineInfo(
    id                = dbObj.as[String]("_id"),
    name              = dbObj.as[String]("name"),
    description       = dbObj.getAs[String]("description"),
    defaultsettings   = MongoUtils.dbObjToMap(dbObj.as[DBObject]("defaultsettings")),
    defaultalgoinfoid = dbObj.as[String]("defaultalgoinfoid"))

  def insert(EngineInfo: EngineInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id"               -> EngineInfo.id,
      "name"              -> EngineInfo.name,
      "defaultsettings"   -> EngineInfo.defaultsettings,
      "defaultalgoinfoid" -> EngineInfo.defaultalgoinfoid)

    // optional fields
    val optObj = EngineInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ optObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToEngineInfo(_) }

  def update(EngineInfo: EngineInfo) = {
    val idObj = MongoDBObject("_id" -> EngineInfo.id)
    val requiredObj = MongoDBObject(
      "name"              -> EngineInfo.name,
      "defaultsettings"   -> EngineInfo.defaultsettings,
      "defaultalgoinfoid" -> EngineInfo.defaultalgoinfoid)
    val descriptionObj = EngineInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
