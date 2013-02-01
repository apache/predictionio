package io.prediction.commons.appdata.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.appdata.{U2IAction, U2IActions}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import org.scala_tools.time.Imports._

/** MongoDB implementation of Items. */
class MongoU2IActions(db: MongoDB) extends U2IActions {
  private val emptyObj = MongoDBObject()
  private val itemColl = db("u2iActions")

  RegisterJodaTimeConversionHelpers()

  private def dbId(appid: Int, id: String) = appid + "_" + id

  def insert(u2iAction: U2IAction) = {
    val appid = MongoDBObject("appid" -> u2iAction.appid)
    val action = MongoDBObject("action" -> u2iAction.action)
    val uid = MongoDBObject("uid" -> dbId(u2iAction.appid, u2iAction.uid))
    val iid = MongoDBObject("iid" -> dbId(u2iAction.appid, u2iAction.iid))
    val t = MongoDBObject("t" -> u2iAction.t)
    val lnglat = u2iAction.latlng map { l => MongoDBObject("lnglat" -> MongoDBList(l._2, l._1)) } getOrElse emptyObj
    val v = u2iAction.v map { v => MongoDBObject("v" -> v) } getOrElse emptyObj
    val price = u2iAction.price map { p => MongoDBObject("price" -> p) } getOrElse emptyObj
    val evalid = u2iAction.evalid map { e => MongoDBObject("evalid" -> e) } getOrElse emptyObj
    itemColl.insert(appid ++ action ++ uid ++ iid ++ t ++ lnglat ++ v ++ price ++ evalid)
  }

  def getAll(appid: Int) = new MongoU2IActionIterator(itemColl.find(MongoDBObject("appid" -> appid)))

  private def dbObjToItem(dbObj: DBObject) = {
    val appid = dbObj.as[Int]("appid")
    U2IAction(
      appid  = appid,
      action = dbObj.as[Int]("action"),
      uid    = dbObj.as[String]("uid").drop(appid.toString.length + 1),
      iid    = dbObj.as[String]("iid").drop(appid.toString.length + 1),
      t      = dbObj.as[DateTime]("t"),
      latlng = dbObj.getAs[MongoDBList]("lnglat") map { lnglat => (lnglat(1).asInstanceOf[Double], lnglat(0).asInstanceOf[Double]) },
      v      = dbObj.getAs[Int]("v"),
      price  = dbObj.getAs[Double]("price"),
      evalid = dbObj.getAs[Int]("evalid")
    )
  }

  class MongoU2IActionIterator(it: MongoCursor) extends Iterator[U2IAction] {
    def next = dbObjToItem(it.next)
    def hasNext = it.hasNext
  }
}
