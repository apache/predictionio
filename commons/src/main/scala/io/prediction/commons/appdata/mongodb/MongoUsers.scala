package io.prediction.commons.appdata.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.appdata.{User, Users}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import org.scala_tools.time.Imports._

/** MongoDB implementation of Users. */
class MongoUsers(db: MongoDB) extends Users {
  private val emptyObj = MongoDBObject()
  private val userColl = db("users")

  RegisterJodaTimeConversionHelpers()

  private def dbId(appid: Int, id: String): String = appid + "_" + id

  def insert(user: User) = {
    val id = MongoDBObject("_id" -> dbId(user.appid, user.id))
    val appid = MongoDBObject("appid" -> user.appid)
    val ct = MongoDBObject("ct" -> user.ct)
    val lnglat = user.latlng map { l => MongoDBObject("lnglat" -> MongoDBList(l._2, l._1)) } getOrElse emptyObj
    val inactive = user.inactive map { i => MongoDBObject("inactive" -> i) } getOrElse emptyObj
    val attributes = user.attributes map { a => MongoDBObject("attributes" -> a.asDBObject) } getOrElse emptyObj
    userColl.insert(id ++ appid ++ ct ++ lnglat ++ inactive ++ attributes)
  }

  def get(appid: Int, id: String) = {
    userColl.findOne(MongoDBObject("_id" -> dbId(appid, id))) map { dbObjToUser(_) }
  }

  def update(user: User) = {
    val id = MongoDBObject("_id" -> dbId(user.appid, user.id))
    val appid = MongoDBObject("appid" -> user.appid)
    val ct = MongoDBObject("ct" -> user.ct)
    val lnglat = user.latlng map { l => MongoDBObject("lnglat" -> MongoDBList(l._2, l._1)) } getOrElse emptyObj
    val inactive = user.inactive map { i => MongoDBObject("inactive" -> i) } getOrElse emptyObj
    val attributes = user.attributes map { a => MongoDBObject("attributes" -> a.asDBObject) } getOrElse emptyObj
    userColl.update(id, id ++ appid ++ ct ++ lnglat ++ inactive ++ attributes)
  }

  def delete(appid: Int, id: String) = userColl.remove(MongoDBObject("_id" -> dbId(appid, id)))
  def delete(user: User) = delete(user.appid, user.id)

  private def dbObjToUser(dbObj: DBObject) = {
    val appid = dbObj.as[Int]("appid")
    User(
      id         = dbObj.as[String]("_id").drop(appid.toString.length + 1),
      appid      = appid,
      ct         = dbObj.as[DateTime]("ct"),
      latlng     = dbObj.getAs[MongoDBList]("lnglat") map { lnglat => (lnglat(1).asInstanceOf[Double], lnglat(0).asInstanceOf[Double]) },
      inactive   = dbObj.getAs[Boolean]("inactive"),
      attributes = dbObj.getAs[DBObject]("attributes") map { MongoUtils.dbObjToMap(_) }
    )
  }
}
