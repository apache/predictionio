package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.modeldata.{ItemRecScore, ItemRecScores}
import com.mongodb.casbah.Imports._

class MongoItemRecScores(db: MongoDB) extends ItemRecScores {
  private val itemRecCol = db("itemRecScores")

  /** Get top N Item Recommendations */
  def get(appid: Int, uid: String, n: Int, modelset: Boolean) = new MongoItemRecScoreIterator(
    itemRecCol.find(MongoDBObject("uid" -> dbId(appid, uid), "modelset" -> modelset))
      .sort(MongoDBObject("score" -> -1))
      .limit(n),
    appid
  ).toList

  /**Get all Item Recommendations */
  def getAll(appid: Int, uid: String, modelset: Boolean) = new MongoItemRecScoreIterator(
    itemRecCol.find(MongoDBObject("uid" -> dbId(appid, uid), "modelset" -> modelset)),
    appid
  )

  /** Insert an ItemSimScore */
  def insert(itemrecscore: ItemRecScore) = {
    val itemRecObj = MongoDBObject(
      "uid" -> dbId(itemrecscore.appid, itemrecscore.uid),
      "iid" -> dbId(itemrecscore.appid, itemrecscore.iid),
      "score" -> itemrecscore.score,
      "itypes" -> itemrecscore.itypes,
      "algoid" -> itemrecscore.algoid,
      "modelset" -> itemrecscore.modelset
    )
    itemRecCol.insert(itemRecObj)
  }

  /** Private mapping function to map DB Object to ItemRecScore object */
  private def dbObjToItemRecScore(dbObj: DBObject, appid: Int) = {
    ItemRecScore(
      uid = dbObj.as[String]("uid").drop(appid.toString.length+1),
      iid = dbObj.as[String]("iid").drop(appid.toString.length+1),
      score = dbObj.as[Double]("score"),
      itypes = getStringList(dbObj.as[MongoDBList]("itypes")),
      appid = appid,
      algoid = dbObj.as[Int]("algoid"),
      modelset = dbObj.as[Boolean]("modelset")
    )
  }

  private def getStringList(dbList: MongoDBList) = {
    val list =dbList.toList
    list collect { case s: String => s}
  }

  private def dbId(appid: Int, id: String) = appid + "_" + id

  class MongoItemRecScoreIterator(it: MongoCursor, appid: Int) extends Iterator[ItemRecScore] {
    def hasNext = it.hasNext
    def next = dbObjToItemRecScore(it.next, appid)
  }
}
