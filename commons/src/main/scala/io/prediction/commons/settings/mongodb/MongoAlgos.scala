package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{Algo, Algos}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.github.nscala_time.time.Imports._

/** MongoDB implementation of Algos. */
class MongoAlgos(db: MongoDB) extends Algos {
  private val emptyObj = MongoDBObject()
  private val algoColl = db("algos")
  private val seq = new MongoSequences(db)
  private val getFields = MongoDBObject(
    "engineid" -> 1,
    "name"     -> 1,
    "infoid"   -> 1,
    "pkgname"  -> 1,
    "deployed" -> 1,
    "command"  -> 1,
    "params"   -> 1,
    "settings" -> 1,
    "modelset" -> 1,
    "createtime" -> 1,
    "updatetime" -> 1,
    "offlineevalid" -> 1
  )

  RegisterJodaTimeConversionHelpers()

  private def dbObjToAlgo(dbObj: DBObject) = {
    Algo(
      id       = dbObj.as[Int]("_id"),
      engineid = dbObj.as[Int]("engineid"),
      name     = dbObj.as[String]("name"),
      infoid   = dbObj.getAs[String]("infoid").getOrElse("pdio-knnitembased"), // TODO: tempararily default for backward compatiblity
      pkgname  = dbObj.as[String]("pkgname"),
      deployed = dbObj.as[Boolean]("deployed"),
      command  = dbObj.as[String]("command"),
      params   = MongoUtils.dbObjToMap(dbObj.as[DBObject]("params")),
      settings = MongoUtils.dbObjToMap(dbObj.as[DBObject]("settings")),
      modelset = dbObj.as[Boolean]("modelset"),
      createtime = dbObj.as[DateTime]("createtime"),
      updatetime = dbObj.as[DateTime]("updatetime"),
      offlineevalid = dbObj.getAs[Int]("offlineevalid")
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
      "pkgname"  -> algo.pkgname,
      "deployed" -> algo.deployed,
      "command"  -> algo.command,
      "params"   -> algo.params,
      "settings" -> algo.settings,
      "modelset" -> algo.modelset,
      "createtime" -> algo.createtime,
      "updatetime" -> algo.updatetime
    )

    // optional fields
    val optObj = algo.offlineevalid.map(x => MongoDBObject("offlineevalid" -> x)).getOrElse(emptyObj)

    algoColl.insert(obj ++ optObj)

    id
  }

  def get(id: Int) = algoColl.findOne(MongoDBObject("_id" -> id), getFields) map { dbObjToAlgo(_) }

  def getByEngineid(engineid: Int) = new MongoAlgoIterator(
    algoColl.find(MongoDBObject("engineid" -> engineid), getFields).sort(MongoDBObject("name" -> 1))
  )

  def getDeployedByEngineid(engineid: Int) = new MongoAlgoIterator(
    algoColl.find(MongoDBObject("engineid" -> engineid, "deployed" -> true), getFields).sort(MongoDBObject("name" -> 1))
  )

  def getByOfflineEvalid(evalid: Int) = new MongoAlgoIterator(
    algoColl.find(MongoDBObject("offlineevalid" -> evalid), getFields).sort(MongoDBObject("name" -> 1))
  )

  def update(algo: Algo) = {

    // required fields
    val obj = MongoDBObject(
      "engineid" -> algo.engineid,
      "name"     -> algo.name,
      "infoid"   -> algo.infoid,
      "pkgname"  -> algo.pkgname,
      "deployed" -> algo.deployed,
      "command"  -> algo.command,
      "params"   -> algo.params,
      "settings" -> algo.settings,
      "modelset" -> algo.modelset,
      "createtime" -> algo.createtime,
      "updatetime" -> algo.updatetime
    )

    // optional fields
    val optObj = algo.offlineevalid.map(x => MongoDBObject("offlineevalid" -> x)).getOrElse(emptyObj)

    algoColl.update(MongoDBObject("_id" -> algo.id), obj ++ optObj)
  }

  def delete(id: Int) = algoColl.remove(MongoDBObject("_id" -> id))

  def existsByEngineidAndName(engineid: Int, name: String) = algoColl.findOne(MongoDBObject("name" -> name, "engineid" -> engineid)) map { _ => true } getOrElse false

  class MongoAlgoIterator(it: MongoCursor) extends Iterator[Algo] {
    def next = dbObjToAlgo(it.next)
    def hasNext = it.hasNext
  }
}
