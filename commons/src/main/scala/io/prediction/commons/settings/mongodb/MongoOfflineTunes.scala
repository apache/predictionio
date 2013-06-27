package io.prediction.commons.settings.mongodb

import io.prediction.commons.settings.{OfflineTune, OfflineTunes}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.github.nscala_time.time.Imports._

class MongoOfflineTunes(db: MongoDB) extends OfflineTunes {

  private val emptyObj = MongoDBObject()
  private val offlineTuneColl = db("offlineTunes")
  private val seq = new MongoSequences(db)
  private def genNextId = seq.genNext("offlineTuneid")
  private val getFields = MongoDBObject( // fields to be read
    "engineid" -> 1,
    "loops" -> 1,
    "createtime" -> 1,
    "starttime" -> 1,
    "endtime" -> 1
  )

  RegisterJodaTimeConversionHelpers()

  private def dbObjToOfflineTune(dbObj: DBObject) = {
    OfflineTune(
      id = dbObj.as[Int]("_id"),
      engineid = dbObj.as[Int]("engineid"),
      loops = dbObj.as[Int]("loops"),
      createtime = dbObj.getAs[DateTime]("createtime"),
      starttime = dbObj.getAs[DateTime]("starttime"),
      endtime = dbObj.getAs[DateTime]("endtime")
    )
  }

  class MongoOfflineTuneIterator(it: MongoCursor) extends Iterator[OfflineTune] {
    def next = dbObjToOfflineTune(it.next)
    def hasNext = it.hasNext
  }

  def insert(offlineTune: OfflineTune): Int = {
    val id = genNextId

    val obj = MongoDBObject(
      "_id" -> id,
      "engineid" -> offlineTune.engineid,
      "loops" -> offlineTune.loops
    )

    // optional fields
    val createtimeObj = offlineTune.createtime.map(x => MongoDBObject("createtime" -> x)).getOrElse(emptyObj)
    val starttimeObj = offlineTune.starttime.map(x => MongoDBObject("starttime" -> x)).getOrElse(emptyObj)
    val endtimeObj = offlineTune.endtime.map(x => MongoDBObject("endtime" -> x)).getOrElse(emptyObj)

    val optObj = createtimeObj ++ starttimeObj ++ endtimeObj

    offlineTuneColl.insert(obj ++ optObj)

    id
  }

  def get(id: Int): Option[OfflineTune] = {
    offlineTuneColl.findOne(MongoDBObject("_id" -> id), getFields) map {dbObjToOfflineTune(_)}
  }

  def getAll() = new MongoOfflineTuneIterator(offlineTuneColl.find())

  def getByEngineid(engineid: Int): Iterator[OfflineTune] = new MongoOfflineTuneIterator(offlineTuneColl.find(MongoDBObject("engineid" -> engineid), getFields))

  def update(offlineTune: OfflineTune, upsert: Boolean = false) = {

    val obj = MongoDBObject(
      "_id" -> offlineTune.id,
      "engineid" -> offlineTune.engineid,
      "loops" -> offlineTune.loops
    )

    // optional fields
    val createtimeObj = offlineTune.createtime.map(x => MongoDBObject("createtime" -> x)).getOrElse(emptyObj)
    val starttimeObj = offlineTune.starttime.map(x => MongoDBObject("starttime" -> x)).getOrElse(emptyObj)
    val endtimeObj = offlineTune.endtime.map(x => MongoDBObject("endtime" -> x)).getOrElse(emptyObj)

    val optObj = createtimeObj ++ starttimeObj ++ endtimeObj

    offlineTuneColl.update(MongoDBObject("_id" -> offlineTune.id), obj ++ optObj, upsert)
  }

  def delete(id: Int) = {
    offlineTuneColl.remove(MongoDBObject("_id" -> id))
  }
}
