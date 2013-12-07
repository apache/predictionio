package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ EngineInfo, EngineInfos }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of EngineInfos. */
class MongoEngineInfos(db: MongoDB) extends EngineInfos {
  private val coll = db("engineInfos")

  private def dbObjToEngineInfo(dbObj: DBObject) = EngineInfo(
    id = dbObj.as[String]("_id"),
    name = dbObj.as[String]("name"),
    description = dbObj.getAs[String]("description"),
    params = (dbObj.as[DBObject]("params") map { p => (p._1, MongoParam.dbObjToParam(p._1, p._2.asInstanceOf[DBObject])) }).toMap,
    paramsections = dbObj.as[Seq[DBObject]]("paramsections") map { MongoParam.dbObjToParamSection(_) },
    defaultalgoinfoid = dbObj.as[String]("defaultalgoinfoid"))

  def insert(engineInfo: EngineInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id" -> engineInfo.id,
      "name" -> engineInfo.name,
      "params" -> (engineInfo.params mapValues { MongoParam.paramToDBObj(_) }),
      "paramsections" -> (engineInfo.paramsections map { MongoParam.paramSectionToDBObj(_) }),
      "defaultalgoinfoid" -> engineInfo.defaultalgoinfoid)

    // optional fields
    val optObj = engineInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ optObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToEngineInfo(_) }

  def getAll() = coll.find().toSeq map { dbObjToEngineInfo(_) }

  def update(engineInfo: EngineInfo, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> engineInfo.id)
    val requiredObj = MongoDBObject(
      "name" -> engineInfo.name,
      "params" -> (engineInfo.params mapValues { MongoParam.paramToDBObj(_) }),
      "paramsections" -> (engineInfo.paramsections map { MongoParam.paramSectionToDBObj(_) }),
      "defaultalgoinfoid" -> engineInfo.defaultalgoinfoid)
    val descriptionObj = engineInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj, upsert)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
