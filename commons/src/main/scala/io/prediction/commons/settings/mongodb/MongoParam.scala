package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.Param

import com.mongodb.casbah.Imports._

/** MongoDB implementation of Param. */
object MongoParam {
  def dbObjToParam(id: String, dbObj: DBObject) = {
    Param(
      id = id,
      name = dbObj.as[String]("name"),
      description = dbObj.getAs[String]("description"),
      defaultvalue = dbObj("defaultvalue"),
      constraint = dbObj.as[String]("constraint"),
      scopes = dbObj.getAs[MongoDBList]("scopes") map { MongoUtils.mongoDbListToListOfString(_).toSet })
  }

  def paramToDBObj(param: Param) = {
    MongoDBObject(
      "name" -> param.name,
      "defaultvalue" -> param.defaultvalue,
      "constraint" -> param.constraint) ++
        (param.description map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj) ++
        (param.scopes map { s => MongoDBObject("scopes" -> s.toSeq) } getOrElse MongoUtils.emptyObj)
  }
}
