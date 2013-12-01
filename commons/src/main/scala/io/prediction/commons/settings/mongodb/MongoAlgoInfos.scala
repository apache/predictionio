package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{AlgoInfo, AlgoInfos, Param}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of AlgoInfos. */
class MongoAlgoInfos(db: MongoDB) extends AlgoInfos {
  private val coll = db("algoInfos")

  private def dbObjToAlgoInfo(dbObj: DBObject) = {
    AlgoInfo(
      id                  = dbObj.as[String]("_id"),
      name                = dbObj.as[String]("name"),
      description         = dbObj.getAs[String]("description"),
      batchcommands       = dbObj.getAs[MongoDBList]("batchcommands") map { MongoUtils.mongoDbListToListOfString(_) },
      offlineevalcommands = dbObj.getAs[MongoDBList]("offlineevalcommands") map { MongoUtils.mongoDbListToListOfString(_) },
      params              = (dbObj.as[DBObject]("params") map { p => (p._1, MongoParam.dbObjToParam(p._1, p._2.asInstanceOf[DBObject])) }).toMap,
      paramsections       = dbObj.as[Seq[DBObject]]("paramsections") map { MongoParam.dbObjToParamSection(_) },
      paramorder          = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("paramorder")),
      engineinfoid        = dbObj.as[String]("engineinfoid"),
      techreq             = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("techreq")),
      datareq             = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("datareq")))
  }

  def insert(algoInfo: AlgoInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id"              -> algoInfo.id,
      "name"             -> algoInfo.name,
      "params"           -> (algoInfo.params mapValues { MongoParam.paramToDBObj(_) }),
      "paramsections"    -> (algoInfo.paramsections map { MongoParam.paramSectionToDBObj(_) }),
      "paramorder"       -> algoInfo.paramorder,
      "engineinfoid"     -> algoInfo.engineinfoid,
      "techreq"          -> algoInfo.techreq,
      "datareq"          -> algoInfo.datareq)

    // optional fields
    val descriptionObj = algoInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val batchcommandsObj = algoInfo.batchcommands.map { c => MongoDBObject("batchcommands" -> c) } getOrElse MongoUtils.emptyObj
    val offlineevalcommandsObj = algoInfo.offlineevalcommands.map { c => MongoDBObject("offlineevalcommands" -> c) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ descriptionObj ++ batchcommandsObj ++ offlineevalcommandsObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToAlgoInfo(_) }

  def getAll() = coll.find().toSeq map { dbObjToAlgoInfo(_) }

  def getByEngineInfoId(engineinfoid: String) = coll.find(MongoDBObject("engineinfoid" -> engineinfoid)).sort(MongoDBObject("_id" -> 1)).toSeq map { dbObjToAlgoInfo(_) }

  def update(algoInfo: AlgoInfo, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> algoInfo.id)
    val requiredObj = MongoDBObject(
      "name"             -> algoInfo.name,
      "params"           -> (algoInfo.params mapValues { MongoParam.paramToDBObj(_) }),
      "paramsections"    -> (algoInfo.paramsections map { MongoParam.paramSectionToDBObj(_) }),
      "paramorder"       -> algoInfo.paramorder,
      "engineinfoid"     -> algoInfo.engineinfoid,
      "techreq"          -> algoInfo.techreq,
      "datareq"          -> algoInfo.datareq)
    val descriptionObj = algoInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val batchcommandsObj = algoInfo.batchcommands.map { c => MongoDBObject("batchcommands" -> c) } getOrElse MongoUtils.emptyObj
    val offlineevalcommandsObj = algoInfo.offlineevalcommands.map { c => MongoDBObject("offlineevalcommands" -> c) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj ++ batchcommandsObj ++ offlineevalcommandsObj, upsert)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
