package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ Engine, Engines }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of Engines. */
class MongoEngines(db: MongoDB) extends Engines {
  private val engineColl = db("engines")
  private val seq = new MongoSequences(db)

  private def dbObjToEngine(dbObj: DBObject) = {
    /** Transparent upgrade. Remove in next minor version. */
    dbObj.getAs[DBObject]("settings") map { settings =>
      val e = Engine(
        id = dbObj.as[Int]("_id"),
        appid = dbObj.as[Int]("appid"),
        name = dbObj.as[String]("name"),
        infoid = dbObj.as[String]("infoid"),
        itypes = dbObj.getAs[MongoDBList]("itypes") map { MongoUtils.mongoDbListToListOfString(_) },
        params = MongoUtils.dbObjToMap(settings),
        trainingdisabled = dbObj.getAs[Boolean]("trainingdisabled"),
        trainingschedule = dbObj.getAs[String]("trainingschedule"))
      update(e)
      e
    } getOrElse {
      Engine(
        id = dbObj.as[Int]("_id"),
        appid = dbObj.as[Int]("appid"),
        name = dbObj.as[String]("name"),
        infoid = dbObj.as[String]("infoid"),
        itypes = dbObj.getAs[MongoDBList]("itypes") map { MongoUtils.mongoDbListToListOfString(_) },
        params = MongoUtils.dbObjToMap(dbObj.as[DBObject]("params")),
        trainingdisabled = dbObj.getAs[Boolean]("trainingdisabled"),
        trainingschedule = dbObj.getAs[String]("trainingschedule"))
    }
  }

  class MongoEngineIterator(it: MongoCursor) extends Iterator[Engine] {
    def next = dbObjToEngine(it.next)
    def hasNext = it.hasNext
  }

  def insert(engine: Engine) = {
    val id = seq.genNext("engineid")

    // required fields
    val obj = MongoDBObject(
      "_id" -> id,
      "appid" -> engine.appid,
      "name" -> engine.name,
      "infoid" -> engine.infoid,
      "params" -> engine.params
    )

    // optional fields
    val optObj = engine.itypes.map(x => MongoDBObject("itypes" -> x)).getOrElse(MongoUtils.emptyObj) ++
      engine.trainingdisabled.map(x => MongoDBObject("trainingdisabled" -> x)).getOrElse(MongoUtils.emptyObj) ++
      engine.trainingschedule.map(x => MongoDBObject("trainingschedule" -> x)).getOrElse(MongoUtils.emptyObj)

    engineColl.insert(obj ++ optObj)

    id
  }

  def get(id: Int) = engineColl.findOne(MongoDBObject("_id" -> id)) map { dbObjToEngine(_) }

  def getAll() = new MongoEngineIterator(engineColl.find())

  def getByAppid(appid: Int) = new MongoEngineIterator(engineColl.find(MongoDBObject("appid" -> appid)).sort(MongoDBObject("name" -> 1)))

  def getByAppidAndName(appid: Int, name: String) = engineColl.findOne(MongoDBObject("appid" -> appid, "name" -> name)) map { dbObjToEngine(_) }

  def getByIdAndAppid(id: Int, appid: Int): Option[Engine] = engineColl.findOne(MongoDBObject("_id" -> id, "appid" -> appid)) map { dbObjToEngine(_) }

  def update(engine: Engine, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> engine.id)
    val nameObj = MongoDBObject("name" -> engine.name)
    val appidObj = MongoDBObject("appid" -> engine.appid)
    val infoidObj = MongoDBObject("infoid" -> engine.infoid)
    val itypesObj = engine.itypes.map(x => MongoDBObject("itypes" -> x)).getOrElse(MongoUtils.emptyObj)
    val paramsObj = MongoDBObject("params" -> engine.params)
    val trainingdisabledObj = engine.trainingdisabled.map(x => MongoDBObject("trainingdisabled" -> x)).getOrElse(MongoUtils.emptyObj)
    val trainingscheduleObj = engine.trainingschedule.map(x => MongoDBObject("trainingschedule" -> x)).getOrElse(MongoUtils.emptyObj)

    engineColl.update(
      idObj,
      idObj ++ appidObj ++ nameObj ++ infoidObj ++ itypesObj ++ paramsObj ++ trainingdisabledObj ++ trainingscheduleObj,
      upsert
    )
  }

  def deleteByIdAndAppid(id: Int, appid: Int) = engineColl.remove(MongoDBObject("_id" -> id, "appid" -> appid))

  def existsByAppidAndName(appid: Int, name: String) = engineColl.findOne(MongoDBObject("name" -> name, "appid" -> appid)) map { _ => true } getOrElse false
}
