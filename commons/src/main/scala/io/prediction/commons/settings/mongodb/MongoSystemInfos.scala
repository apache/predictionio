package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ SystemInfo, SystemInfos }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of SystemInfos. */
class MongoSystemInfos(db: MongoDB) extends SystemInfos {
  private val coll = db("systemInfos")

  private def dbObjToSystemInfo(dbObj: DBObject) = {
    SystemInfo(
      id = dbObj.as[String]("_id"),
      value = dbObj.as[String]("value"),
      description = dbObj.getAs[String]("description"))
  }

  def insert(systemInfo: SystemInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id" -> systemInfo.id,
      "value" -> systemInfo.value)

    // optional fields
    val optObj = systemInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ optObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToSystemInfo(_) }

  def getAll = coll.find().toSeq map { dbObjToSystemInfo(_) }

  def update(systemInfo: SystemInfo, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> systemInfo.id)
    val valueObj = MongoDBObject("value" -> systemInfo.value)
    val descriptionObj = systemInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ valueObj ++ descriptionObj, upsert)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
