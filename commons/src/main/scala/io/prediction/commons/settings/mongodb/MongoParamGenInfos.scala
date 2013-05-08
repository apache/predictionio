package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ParamGenInfo, ParamGenInfos}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of ParamGenInfos. */
class MongoParamGenInfos(db: MongoDB) extends ParamGenInfos {
  private val coll = db("paramGenInfos")

  private def dbObjToParamGenInfo(dbObj: DBObject) = {
    val params = dbObj.as[MongoDBList]("params")
    val paramorder = params map { p => p.asInstanceOf[DBObject].as[String]("param") }
    val paramdefaults = params map { p => p.asInstanceOf[DBObject].as[Any]("default") }
    val paramnames = params map { p => p.asInstanceOf[DBObject].as[String]("name") }
    val paramdescription = params map { p => p.asInstanceOf[DBObject].as[String]("description") }
    ParamGenInfo(
      id               = dbObj.as[String]("_id"),
      name             = dbObj.as[String]("name"),
      description      = dbObj.getAs[String]("description"),
      commands         = dbObj.getAs[MongoDBList]("commands") map { MongoUtils.mongoDbListToListOfString(_) },
      paramdefaults    = Map() ++ (paramorder zip paramdefaults),
      paramnames       = Map() ++ (paramorder zip paramnames),
      paramdescription = Map() ++ (paramorder zip paramdescription),
      paramorder       = paramorder)
  }

  private def mergeParams(order: Seq[String], names: Map[String, String], defaults: Map[String, Any], description: Map[String, String]): Seq[Map[String, Any]] = {
    val listBuffer = collection.mutable.ListBuffer[Map[String, Any]]()

    order foreach { k =>
      listBuffer += Map("param" -> k, "default" -> defaults(k), "name" -> names(k), "description" -> description(k))
    }

    listBuffer.toSeq
  }

  def insert(ParamGenInfo: ParamGenInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id"              -> ParamGenInfo.id,
      "name"             -> ParamGenInfo.name,
      "params"           -> mergeParams(ParamGenInfo.paramorder, ParamGenInfo.paramnames, ParamGenInfo.paramdefaults, ParamGenInfo.paramdescription))

    // optional fields
    val descriptionObj = ParamGenInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = ParamGenInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ descriptionObj ++ commandsObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToParamGenInfo(_) }

  def update(ParamGenInfo: ParamGenInfo) = {
    val idObj = MongoDBObject("_id" -> ParamGenInfo.id)
    val requiredObj = MongoDBObject(
      "name"             -> ParamGenInfo.name,
      "params"           -> mergeParams(ParamGenInfo.paramorder, ParamGenInfo.paramnames, ParamGenInfo.paramdefaults, ParamGenInfo.paramdescription))
    val descriptionObj = ParamGenInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = ParamGenInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj ++ commandsObj)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
