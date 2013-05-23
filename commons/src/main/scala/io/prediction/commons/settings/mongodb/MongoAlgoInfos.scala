package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{AlgoInfo, AlgoInfos}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of OfflineEvalMetricInfos. */
class MongoAlgoInfos(db: MongoDB) extends AlgoInfos {
  private val coll = db("algoInfos")

  private def dbObjToAlgoInfo(dbObj: DBObject) = {
    val params = dbObj.as[MongoDBList]("params")
    val paramorder = params map { p => p.asInstanceOf[DBObject].as[String]("param") }
    val paramdefaults = params map { p => p.asInstanceOf[DBObject].as[Any]("default") }
    val paramnames = params map { p => p.asInstanceOf[DBObject].as[String]("name") }
    val paramdescription = params map { p => p.asInstanceOf[DBObject].as[String]("description") }
    AlgoInfo(
      id                  = dbObj.as[String]("_id"),
      name                = dbObj.as[String]("name"),
      description         = dbObj.getAs[String]("description"),
      batchcommands       = dbObj.getAs[MongoDBList]("batchcommands") map { MongoUtils.mongoDbListToListOfString(_) },
      offlineevalcommands = dbObj.getAs[MongoDBList]("offlineevalcommands") map { MongoUtils.mongoDbListToListOfString(_) },
      paramdefaults       = Map() ++ (paramorder zip paramdefaults),
      paramnames          = Map() ++ (paramorder zip paramnames),
      paramdescription    = Map() ++ (paramorder zip paramdescription),
      paramorder          = paramorder,
      engineinfoid        = dbObj.as[String]("engineinfoid"),
      techreq             = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("techreq")),
      datareq             = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("datareq")))
  }

  private def mergeParams(order: Seq[String], names: Map[String, String], defaults: Map[String, Any], description: Map[String, String]): Seq[Map[String, Any]] = {
    val listBuffer = collection.mutable.ListBuffer[Map[String, Any]]()

    order foreach { k =>
      listBuffer += Map("param" -> k, "default" -> defaults(k), "name" -> names(k), "description" -> description(k))
    }

    listBuffer.toSeq
  }

  def insert(algoInfo: AlgoInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id"          -> algoInfo.id,
      "name"         -> algoInfo.name,
      "params"       -> mergeParams(algoInfo.paramorder, algoInfo.paramnames, algoInfo.paramdefaults, algoInfo.paramdescription),
      "engineinfoid" -> algoInfo.engineinfoid,
      "techreq"      -> algoInfo.techreq,
      "datareq"      -> algoInfo.datareq)

    // optional fields
    val descriptionObj = algoInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val batchcommandsObj = algoInfo.batchcommands.map { c => MongoDBObject("batchcommands" -> c) } getOrElse MongoUtils.emptyObj
    val offlineevalcommandsObj = algoInfo.offlineevalcommands.map { c => MongoDBObject("offlineevalcommands" -> c) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ descriptionObj ++ batchcommandsObj ++ offlineevalcommandsObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToAlgoInfo(_) }

  def getByEngineInfoId(engineinfoid: String) = coll.find(MongoDBObject("engineinfoid" -> engineinfoid)).toSeq map { dbObjToAlgoInfo(_) }

  def update(algoInfo: AlgoInfo) = {
    val idObj = MongoDBObject("_id" -> algoInfo.id)
    val requiredObj = MongoDBObject(
      "name"         -> algoInfo.name,
      "params"       -> mergeParams(algoInfo.paramorder, algoInfo.paramnames, algoInfo.paramdefaults, algoInfo.paramdescription),
      "engineinfoid" -> algoInfo.engineinfoid,
      "techreq"      -> algoInfo.techreq,
      "datareq"      -> algoInfo.datareq)
    val descriptionObj = algoInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val batchcommandsObj = algoInfo.batchcommands.map { c => MongoDBObject("batchcommands" -> c) } getOrElse MongoUtils.emptyObj
    val offlineevalcommandsObj = algoInfo.offlineevalcommands.map { c => MongoDBObject("offlineevalcommands" -> c) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj ++ batchcommandsObj ++ offlineevalcommandsObj)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
