package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{MetricInfo, MetricInfos}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of MetricInfos. */
class MongoMetricInfos(db: MongoDB) extends MetricInfos {
  private val coll = db("metricInfos")

  private def dbObjToMetricInfo(dbObj: DBObject) = {
    val params = dbObj.as[MongoDBList]("params")
    val paramorder = params map { p => p.asInstanceOf[DBObject].as[String]("param") }
    val paramdefaults = params map { p => p.asInstanceOf[DBObject].as[Any]("default") }
    val paramnames = params map { p => p.asInstanceOf[DBObject].as[String]("name") }
    val paramdescription = params map { p => p.asInstanceOf[DBObject].as[String]("description") }
    MetricInfo(
      id               = dbObj.as[String]("_id"),
      name             = dbObj.as[String]("name"),
      description      = dbObj.getAs[String]("description"),
      engineinfoids    = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("engineinfoids")),
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

  def insert(MetricInfo: MetricInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id"              -> MetricInfo.id,
      "name"             -> MetricInfo.name,
      "engineinfoids"    -> MetricInfo.engineinfoids,
      "params"           -> mergeParams(MetricInfo.paramorder, MetricInfo.paramnames, MetricInfo.paramdefaults, MetricInfo.paramdescription))

    // optional fields
    val descriptionObj = MetricInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = MetricInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ descriptionObj ++ commandsObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToMetricInfo(_) }

  def update(MetricInfo: MetricInfo) = {
    val idObj = MongoDBObject("_id" -> MetricInfo.id)
    val requiredObj = MongoDBObject(
      "name"             -> MetricInfo.name,
      "engineinfoids"    -> MetricInfo.engineinfoids,
      "params"           -> mergeParams(MetricInfo.paramorder, MetricInfo.paramnames, MetricInfo.paramdefaults, MetricInfo.paramdescription))
    val descriptionObj = MetricInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = MetricInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj ++ commandsObj)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
