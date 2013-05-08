package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ParamGen, ParamGens}

import com.mongodb.casbah.Imports._

class MongoParamGens(db: MongoDB) extends ParamGens {

  private val emptyObj = MongoDBObject()
  private val paramGensColl = db("paramGens")
  private val seq = new MongoSequences(db)
  private def genNextId = seq.genNext("paramGenid")

  private val getFields = MongoDBObject( // fields to be read
    "infoid" -> 1,
    "tuneid" -> 1,
    "params" -> 1
  )

  class MongoParamGenIterator(it: MongoCursor) extends Iterator[ParamGen] {
    def next = dbObjToParamGen(it.next)
    def hasNext = it.hasNext
  }

  /** create ParamGen object from DBObject */
  private def dbObjToParamGen(dbObj: DBObject) = {
    ParamGen(
      id = dbObj.as[Int]("_id"),
      infoid = dbObj.as[String]("infoid"),
      tuneid = dbObj.as[Int]("tuneid"),
      params = MongoUtils.dbObjToMap(dbObj.as[DBObject]("params"))
    )
  }

  /** Insert a paramGen and return id */
  def insert(paramGen: ParamGen): Int = {
    val id = genNextId

    paramGensColl.insert(MongoDBObject(
      "_id" -> id,
      "infoid" -> paramGen.infoid,
      "tuneid" -> paramGen.tuneid,
      "params" -> paramGen.params
    ))

    id
  }

  /** Get a paramGen by its ID */
  def get(id: Int): Option[ParamGen] = {
    paramGensColl.findOne(MongoDBObject("_id" -> id), getFields) map { dbObjToParamGen(_) }
  }

  /** Update paramGen */
  def update(paramGen: ParamGen) = {
    paramGensColl.update(MongoDBObject("_id" -> paramGen.id), MongoDBObject(
      "infoid" -> paramGen.infoid,
      "tuneid" -> paramGen.tuneid,
      "params" -> paramGen.params
    ))
  }

  /** Delete paramGen by its ID */
  def delete(id: Int) = {
    paramGensColl.remove(MongoDBObject("_id" -> id))
  }
}
