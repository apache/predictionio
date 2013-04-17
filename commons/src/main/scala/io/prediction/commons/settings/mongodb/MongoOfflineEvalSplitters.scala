package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{OfflineEvalSplitter, OfflineEvalSplitters}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of OfflineEvalSplitters. */
class MongoOfflineEvalSplitters(db: MongoDB) extends OfflineEvalSplitters {
  private val coll = db("offlineEvalSplitters")
  private val seq = new MongoSequences(db)

  private def dbObjToOfflineEvalSplitter(dbObj: DBObject) = {
    OfflineEvalSplitter(
      id       = dbObj.as[Int]("_id"),
      evalid   = dbObj.as[Int]("evalid"),
      name     = dbObj.as[String]("name"),
      infoid   = dbObj.as[String]("infoid"),
      settings = MongoUtils.dbObjToMap(dbObj.as[DBObject]("settings"))
    )
  }

  class MongoOfflineEvalSplitterIterator(it: MongoCursor) extends Iterator[OfflineEvalSplitter] {
    def next = dbObjToOfflineEvalSplitter(it.next)
    def hasNext = it.hasNext
  }

  def insert(splitter: OfflineEvalSplitter) = {
    val id = seq.genNext("offlineEvalSplitterId")

    // required fields
    val obj = MongoDBObject(
      "_id"      -> id,
      "evalid"   -> splitter.evalid,
      "name"     -> splitter.name,
      "infoid"   -> splitter.infoid,
      "settings" -> splitter.settings
    )

    coll.insert(obj)

    id
  }

  def get(id: Int) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToOfflineEvalSplitter(_) }

  def getByEvalid(evalid: Int): Iterator[OfflineEvalSplitter] = new MongoOfflineEvalSplitterIterator(
    coll.find(MongoDBObject("evalid" -> evalid)).sort(MongoDBObject("infoid" -> 1))
  )

  def update(splitter: OfflineEvalSplitter) = {
    val idObj = MongoDBObject("_id" -> splitter.id)
    val evalidObj = MongoDBObject("evalid" -> splitter.evalid)
    val nameObj = MongoDBObject("name" -> splitter.name)
    val infoidObj = MongoDBObject("infoid" -> splitter.infoid)
    val settingsObj = MongoDBObject("settings" -> splitter.settings)

    coll.update(
      idObj,
      evalidObj ++ nameObj ++ infoidObj ++ settingsObj
    )
  }

  def delete(id: Int) = coll.remove(MongoDBObject("_id" -> id))
}
