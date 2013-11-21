package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{Algo, Algos}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.github.nscala_time.time.Imports._

/** MongoDB implementation of Algos. */
class MongoAlgos(db: MongoDB) extends Algos {
  private val algoColl = db("algos")
  private val seq = new MongoSequences(db)
  private val getFields = MongoDBObject(
    "engineid" -> 1,
    "name"     -> 1,
    "infoid"   -> 1,
    "command"  -> 1,
    "params"   -> 1,
    "settings" -> 1,
    "modelset" -> 1,
    "createtime" -> 1,
    "updatetime" -> 1,
    "status" -> 1,
    "offlineevalid" -> 1,
    "offlinetuneid" -> 1,
    "loop" -> 1,
    "paramset" -> 1
  )

  RegisterJodaTimeConversionHelpers()

  private def dbObjToAlgo(dbObj: DBObject) = {
    Algo(
      id       = dbObj.as[Int]("_id"),
      engineid = dbObj.as[Int]("engineid"),
      name     = dbObj.as[String]("name"),
      infoid   = dbObj.getAs[String]("infoid").getOrElse("pdio-knnitembased"), // TODO: tempararily default for backward compatiblity
      command  = dbObj.as[String]("command"),
      params   = MongoUtils.dbObjToMap(dbObj.as[DBObject]("params")),
      settings = MongoUtils.dbObjToMap(dbObj.as[DBObject]("settings")),
      modelset = dbObj.as[Boolean]("modelset"),
      createtime = dbObj.as[DateTime]("createtime"),
      updatetime = dbObj.as[DateTime]("updatetime"),
      status = dbObj.as[String]("status"),
      offlineevalid = dbObj.getAs[Int]("offlineevalid"),
      offlinetuneid = dbObj.getAs[Int]("offlinetuneid"),
      loop = dbObj.getAs[Int]("loop"),
      paramset = dbObj.getAs[Int]("paramset")
    )
  }

  def insert(algo: Algo) = {
    val id = seq.genNext("algoid")

    // required fields
    val obj = MongoDBObject(
      "_id"      -> id,
      "engineid" -> algo.engineid,
      "name"     -> algo.name,
      "infoid"   -> algo.infoid,
      "command"  -> algo.command,
      "params"   -> algo.params,
      "settings" -> algo.settings,
      "modelset" -> algo.modelset,
      "createtime" -> algo.createtime,
      "updatetime" -> algo.updatetime,
      "status" -> algo.status
    )

    // optional fields
    val optObj = algo.offlineevalid.map(x => MongoDBObject("offlineevalid" -> x)).getOrElse(MongoUtils.emptyObj) ++
      algo.offlinetuneid.map(x => MongoDBObject("offlinetuneid" -> x)).getOrElse(MongoUtils.emptyObj) ++
      algo.loop.map(x => MongoDBObject("loop" -> x)).getOrElse(MongoUtils.emptyObj) ++
      algo.paramset.map(x => MongoDBObject("paramset" -> x)).getOrElse(MongoUtils.emptyObj)

    algoColl.insert(obj ++ optObj)

    id
  }

  def get(id: Int) = algoColl.findOne(MongoDBObject("_id" -> id), getFields) map { dbObjToAlgo(_) }

  def getAll() = new MongoAlgoIterator(algoColl.find())

  def getByEngineid(engineid: Int) = new MongoAlgoIterator(
    algoColl.find(MongoDBObject("engineid" -> engineid), getFields).sort(MongoDBObject("name" -> 1))
  )

  def getDeployedByEngineid(engineid: Int) = new MongoAlgoIterator(
    algoColl.find(MongoDBObject("engineid" -> engineid, "status" -> "deployed"), getFields).sort(MongoDBObject("name" -> 1))
  )

  def getByOfflineEvalid(evalid: Int, loop: Option[Int] = None, paramset: Option[Int] = None) = {
    val q = MongoDBObject("offlineevalid" -> evalid) ++ loop.map(l => MongoDBObject("loop" -> l)).getOrElse(MongoUtils.emptyObj) ++ paramset.map(p => MongoDBObject("paramset" -> p)).getOrElse(MongoUtils.emptyObj)
    new MongoAlgoIterator(algoColl.find(q, getFields).sort(MongoDBObject("name" -> 1)))
  }

  def getTuneSubjectByOfflineTuneid(tuneid: Int) = algoColl.findOne(MongoDBObject("offlinetuneid" -> tuneid, "loop" -> null, "paramset" -> null)) map { dbObjToAlgo(_) }

  def update(algo: Algo, upsert: Boolean = false) = {

    // required fields
    val obj = MongoDBObject(
      "_id"        -> algo.id,
      "engineid"   -> algo.engineid,
      "name"       -> algo.name,
      "infoid"     -> algo.infoid,
      "command"    -> algo.command,
      "params"     -> algo.params,
      "settings"   -> algo.settings,
      "modelset"   -> algo.modelset,
      "createtime" -> algo.createtime,
      "updatetime" -> algo.updatetime,
      "status"     -> algo.status)

    // optional fields
    val optObj = algo.offlineevalid.map(x => MongoDBObject("offlineevalid" -> x)).getOrElse(MongoUtils.emptyObj) ++
      algo.offlinetuneid.map(x => MongoDBObject("offlinetuneid" -> x)).getOrElse(MongoUtils.emptyObj) ++
      algo.loop.map(x => MongoDBObject("loop" -> x)).getOrElse(MongoUtils.emptyObj) ++
      algo.paramset.map(x => MongoDBObject("paramset" -> x)).getOrElse(MongoUtils.emptyObj)

    algoColl.update(MongoDBObject("_id" -> algo.id), obj ++ optObj, upsert)
  }

  def delete(id: Int) = algoColl.remove(MongoDBObject("_id" -> id))

  def existsByEngineidAndName(engineid: Int, name: String) = algoColl.findOne(MongoDBObject("name" -> name, "engineid" -> engineid, "offlineevalid" -> null)) map { _ => true } getOrElse false

  class MongoAlgoIterator(it: MongoCursor) extends Iterator[Algo] {
    def next = dbObjToAlgo(it.next)
    def hasNext = it.hasNext
  }
}
