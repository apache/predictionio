package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{Engine, Engines}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of Engines. */
class MongoEngines(db: MongoDB) extends Engines {
  private val engineColl = db("engines")
  private val seq = new MongoSequences(db)
  private val getFields = MongoDBObject("appid" -> 1, "name" -> 1, "enginetype" -> 1, "itypes" -> 1, "settings" -> 1)

  private def dbObjToEngine(dbObj: DBObject) = {
    Engine(
      id         = dbObj.as[Int]("_id"),
      appid      = dbObj.as[Int]("appid"),
      name       = dbObj.as[String]("name"),
      enginetype = dbObj.as[String]("enginetype"),
      itypes     = dbObj.getAs[MongoDBList]("itypes") map { _.toList.map { _.toString } },
      settings   = MongoUtils.dbObjToMap(dbObj.as[DBObject]("settings"))
    )
  }

  class MongoEngineIterator(it: MongoCursor) extends Iterator[Engine] {
    def next = dbObjToEngine(it.next)
    def hasNext = it.hasNext
  }

  def insert(engine: Engine) = {
    val id = seq.genNext("engineid")

    // required fields
    val obj = MongoDBObject(
      "_id"        -> id,
      "appid"      -> engine.appid,
      "name"       -> engine.name,
      "enginetype" -> engine.enginetype,
      "settings"   -> engine.settings
    )

    // optional fields
    val optObj = engine.itypes.map (x => MongoDBObject("itypes" -> x)).getOrElse(MongoUtils.emptyObj)

    engineColl.insert( obj ++ optObj )

    id
  }

  def get(id: Int) = engineColl.findOne(MongoDBObject("_id" -> id), getFields) map { dbObjToEngine(_) }

  def getByAppid(appid: Int) = new MongoEngineIterator(engineColl.find(MongoDBObject("appid" -> appid)).sort(MongoDBObject("name" -> 1)))

  def getByAppidAndName(appid: Int, name: String) = engineColl.findOne(MongoDBObject("appid" -> appid, "name" -> name)) map { dbObjToEngine(_) }

  def update(engine: Engine) = {
    val idObj = MongoDBObject("_id" -> engine.id)
    val nameObj = MongoDBObject("name" -> engine.name)
    val appidObj = MongoDBObject("appid" -> engine.appid)
    val enginetypeObj = MongoDBObject("enginetype" -> engine.enginetype)
    val itypesObj = engine.itypes.map (x => MongoDBObject("itypes" -> x)).getOrElse(MongoUtils.emptyObj)
    val settingsObj = MongoDBObject("settings" -> engine.settings)

    engineColl.update(
      idObj,
      appidObj ++ nameObj ++ enginetypeObj ++ itypesObj ++ settingsObj
    )
  }

  def deleteByIdAndAppid(id: Int, appid: Int) = engineColl.remove(MongoDBObject("_id" -> id, "appid" -> appid))

  def existsByAppidAndName(appid: Int, name: String) = engineColl.findOne(MongoDBObject("name" -> name, "appid" -> appid)) map { _ => true } getOrElse false
}
