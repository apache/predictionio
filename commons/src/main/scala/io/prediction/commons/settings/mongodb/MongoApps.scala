package io.prediction.commons.settings.mongodb

import io.prediction.commons.settings.{ App, Apps }

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.WriteConcern

/** MongoDB implementation of Apps. */
class MongoApps(db: MongoDB) extends Apps {
  private val emptyObj = MongoDBObject()
  private val appColl = db("apps")
  private val seq = new MongoSequences(db)
  private val getFields = MongoDBObject("userid" -> 1, "appkey" -> 1, "display" -> 1, "url" -> 1, "cat" -> 1, "desc" -> 1, "timezone" -> 1)

  appColl.setWriteConcern(WriteConcern.JournalSafe)

  private def dbObjToApp(dbObj: DBObject) = {
    App(
      id = dbObj.as[Int]("_id"),
      userid = dbObj.as[Int]("userid"),
      appkey = dbObj.as[String]("appkey"),
      display = dbObj.as[String]("display"),
      url = dbObj.getAs[String]("url"),
      cat = dbObj.getAs[String]("cat"),
      desc = dbObj.getAs[String]("desc"),
      timezone = dbObj.as[String]("timezone")
    )
  }

  class MongoAppIterator(it: MongoCursor) extends Iterator[App] {
    def next = dbObjToApp(it.next)
    def hasNext = it.hasNext
  }

  def insert(app: App) = {
    val id = seq.genNext("appid")
    val must = MongoDBObject(
      "_id" -> id,
      "userid" -> app.userid,
      "appkey" -> app.appkey,
      "display" -> app.display,
      "timezone" -> app.timezone
    )
    val url = app.url map { url => MongoDBObject("url" -> url) } getOrElse emptyObj
    val cat = app.cat map { cat => MongoDBObject("cat" -> cat) } getOrElse emptyObj
    val desc = app.desc map { desc => MongoDBObject("desc" -> desc) } getOrElse emptyObj

    appColl.insert(must ++ url ++ cat ++ desc)
    id
  }

  def get(id: Int) = appColl.findOne(MongoDBObject("_id" -> id), getFields) map { dbObjToApp(_) }

  def getAll() = new MongoAppIterator(appColl.find())

  def getByUserid(userid: Int) = new MongoAppIterator(appColl.find(MongoDBObject("userid" -> userid), getFields))

  def getByAppkey(appkey: String) = appColl.findOne(MongoDBObject("appkey" -> appkey), getFields) map { dbObjToApp(_) }

  def getByAppkeyAndUserid(appkey: String, userid: Int) = appColl.findOne(MongoDBObject("appkey" -> appkey, "userid" -> userid), getFields) map { dbObjToApp(_) }

  def getByIdAndUserid(id: Int, userid: Int) = appColl.findOne(MongoDBObject("_id" -> id, "userid" -> userid), getFields) map { dbObjToApp(_) }

  def update(app: App, upsert: Boolean = false) = {
    val must = MongoDBObject(
      "_id" -> app.id,
      "userid" -> app.userid,
      "appkey" -> app.appkey,
      "display" -> app.display,
      "timezone" -> app.timezone)
    val url = app.url map { url => MongoDBObject("url" -> url) } getOrElse emptyObj
    val cat = app.cat map { cat => MongoDBObject("cat" -> cat) } getOrElse emptyObj
    val desc = app.desc map { desc => MongoDBObject("desc" -> desc) } getOrElse emptyObj

    appColl.update(MongoDBObject("_id" -> app.id), must ++ url ++ cat ++ desc, upsert)
  }

  def updateAppkeyByAppkeyAndUserid(appkey: String, userid: Int, newAppkey: String) = {
    appColl.findAndModify(MongoDBObject("appkey" -> appkey, "userid" -> userid), MongoDBObject("$set" -> MongoDBObject("appkey" -> newAppkey))) map { dbObjToApp(_) }
  }

  def updateTimezoneByAppkeyAndUserid(appkey: String, userid: Int, timezone: String) = {
    appColl.findAndModify(MongoDBObject("appkey" -> appkey, "userid" -> userid), MongoDBObject("$set" -> MongoDBObject("timezone" -> timezone))) map { dbObjToApp(_) }
  }

  def deleteByIdAndUserid(id: Int, userid: Int) = appColl.remove(MongoDBObject("_id" -> id, "userid" -> userid))

  def existsByIdAndAppkeyAndUserid(id: Int, appkey: String, userid: Int) = appColl.findOne(MongoDBObject("_id" -> id, "appkey" -> appkey, "userid" -> userid)) map { _ => true } getOrElse false
}
