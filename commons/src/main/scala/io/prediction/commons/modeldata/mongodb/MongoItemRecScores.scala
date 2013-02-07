package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.MongoUtils._
import io.prediction.commons.modeldata.{ItemRecScore, ItemRecScores}
import io.prediction.commons.settings.{Algo, App}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of ItemRecScores. */
class MongoItemRecScores(db: MongoDB) extends ItemRecScores {
  private val itemRecScoreColl = db("itemRecScores")

  def getTopN(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo) = new MongoItemRecScoreIterator(
    itemRecScoreColl.find(
      MongoDBObject("algoid" -> algo.id, "modelset" -> algo.modelset, "uid" -> idWithAppid(app.id, uid)) ++
        (itypes map { loi =>
          MongoDBObject("itypes" -> MongoDBObject("$in" -> loi))
        } getOrElse emptyObj)
    ).sort(MongoDBObject("score" -> -1)).limit(n),
    app.id
  )

  def insert(itemrecscore: ItemRecScore) = {
    val itemRecObj = MongoDBObject(
      "uid" -> idWithAppid(itemrecscore.appid, itemrecscore.uid),
      "iid" -> idWithAppid(itemrecscore.appid, itemrecscore.iid),
      "score" -> itemrecscore.score,
      "itypes" -> itemrecscore.itypes,
      "algoid" -> itemrecscore.algoid,
      "modelset" -> itemrecscore.modelset
    )
    itemRecScoreColl.insert(itemRecObj)
  }

  def deleteByAlgoid(algoid: Int) = {
    itemRecScoreColl.remove(MongoDBObject("algoid" -> algoid))
  }
  
  /** Private mapping function to map DB Object to ItemRecScore object */
  private def dbObjToItemRecScore(dbObj: DBObject, appid: Int) = {
    ItemRecScore(
      uid = dbObj.as[String]("uid").drop(appid.toString.length+1),
      iid = dbObj.as[String]("iid").drop(appid.toString.length+1),
      score = dbObj.as[Double]("score"),
      itypes = dbObj.as[MongoDBList]("itypes"),
      appid = appid,
      algoid = dbObj.as[Int]("algoid"),
      modelset = dbObj.as[Boolean]("modelset")
    )
  }

  class MongoItemRecScoreIterator(it: MongoCursor, appid: Int) extends Iterator[ItemRecScore] {
    def hasNext = it.hasNext
    def next = dbObjToItemRecScore(it.next, appid)
  }
}
