package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ OfflineEvalSplitterInfo, OfflineEvalSplitterInfos }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of OfflineEvalSplitterInfos. */
class MongoOfflineEvalSplitterInfos(db: MongoDB) extends OfflineEvalSplitterInfos {
  private val coll = db("offlineEvalSplitterInfos")

  private def dbObjToOfflineEvalSplitterInfo(dbObj: DBObject) = {
    OfflineEvalSplitterInfo(
      id = dbObj.as[String]("_id"),
      name = dbObj.as[String]("name"),
      description = dbObj.getAs[String]("description"),
      engineinfoids = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("engineinfoids")),
      commands = dbObj.getAs[MongoDBList]("commands") map { MongoUtils.mongoDbListToListOfString(_) },
      params = (dbObj.as[DBObject]("params") map { p => (p._1, MongoParam.dbObjToParam(p._1, p._2.asInstanceOf[DBObject])) }).toMap,
      paramsections = dbObj.as[Seq[DBObject]]("paramsections") map { MongoParam.dbObjToParamSection(_) },
      paramorder = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("paramorder")))
  }

  def insert(offlineEvalSplitterInfo: OfflineEvalSplitterInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id" -> offlineEvalSplitterInfo.id,
      "name" -> offlineEvalSplitterInfo.name,
      "engineinfoids" -> offlineEvalSplitterInfo.engineinfoids,
      "params" -> offlineEvalSplitterInfo.params.mapValues { MongoParam.paramToDBObj(_) },
      "paramsections" -> offlineEvalSplitterInfo.paramsections.map { MongoParam.paramSectionToDBObj(_) },
      "paramorder" -> offlineEvalSplitterInfo.paramorder)

    // optional fields
    val descriptionObj = offlineEvalSplitterInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = offlineEvalSplitterInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ descriptionObj ++ commandsObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToOfflineEvalSplitterInfo(_) }

  def getAll() = coll.find().toSeq map { dbObjToOfflineEvalSplitterInfo(_) }

  def getByEngineinfoid(engineinfoid: String): Seq[OfflineEvalSplitterInfo] = {
    coll.find(MongoDBObject("engineinfoids" -> MongoDBObject("$in" -> Seq(engineinfoid)))).toSeq map { dbObjToOfflineEvalSplitterInfo(_) }
  }

  def update(offlineEvalSplitterInfo: OfflineEvalSplitterInfo, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> offlineEvalSplitterInfo.id)
    val requiredObj = MongoDBObject(
      "name" -> offlineEvalSplitterInfo.name,
      "engineinfoids" -> offlineEvalSplitterInfo.engineinfoids,
      "params" -> offlineEvalSplitterInfo.params.mapValues { MongoParam.paramToDBObj(_) },
      "paramsections" -> offlineEvalSplitterInfo.paramsections.map { MongoParam.paramSectionToDBObj(_) },
      "paramorder" -> offlineEvalSplitterInfo.paramorder)

    val descriptionObj = offlineEvalSplitterInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = offlineEvalSplitterInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj ++ commandsObj, upsert)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
