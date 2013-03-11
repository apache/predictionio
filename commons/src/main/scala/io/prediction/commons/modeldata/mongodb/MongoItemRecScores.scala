package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.MongoUtils._
import io.prediction.commons.modeldata.{ItemRecScore, ItemRecScores}
import io.prediction.commons.settings.{Algo, App, OfflineEval}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of ItemRecScores. */
class MongoItemRecScores(db: MongoDB) extends ItemRecScores {
  private val itemRecScoreColl = db("itemRecScores")

  /** Indices and hints. */
  val scoreIdIndex = MongoDBObject("score" -> -1, "_id" -> 1)
  itemRecScoreColl.ensureIndex(scoreIdIndex)
  itemRecScoreColl.ensureIndex(MongoDBObject("algoid" -> 1, "uid" -> 1, "modelset" -> 1))

  def getTopN(uid: String, n: Int, itypes: Option[List[String]], after: Option[ItemRecScore])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None) = {
    val modelset = offlineEval map { _ => false } getOrElse algo.modelset
    val query = MongoDBObject("algoid" -> algo.id, "uid" -> idWithAppid(app.id, uid), "modelset" -> modelset) ++
      (itypes map { loi => MongoDBObject("itypes" -> MongoDBObject("$in" -> loi)) } getOrElse emptyObj)
    after map { irs =>
      new MongoItemRecScoreIterator(
        itemRecScoreColl.find(query).
          $min(MongoDBObject("score" -> irs.score, "_id" -> irs.id)).
          sort(scoreIdIndex).
          skip(1).limit(n),
        app.id
      )
    } getOrElse new MongoItemRecScoreIterator(
      itemRecScoreColl.find(query).sort(scoreIdIndex).limit(n),
      app.id
    )
  }

  def insert(itemrecscore: ItemRecScore) = {
    val id = new ObjectId
    val itemRecObj = MongoDBObject(
      "_id" -> id,
      "uid" -> idWithAppid(itemrecscore.appid, itemrecscore.uid),
      "iid" -> idWithAppid(itemrecscore.appid, itemrecscore.iid),
      "score" -> itemrecscore.score,
      "itypes" -> itemrecscore.itypes,
      "algoid" -> itemrecscore.algoid,
      "modelset" -> itemrecscore.modelset
    )
    itemRecScoreColl.insert(itemRecObj)
    itemrecscore.copy(id = Some(id))
  }

  def deleteByAlgoid(algoid: Int) = {
    itemRecScoreColl.remove(MongoDBObject("algoid" -> algoid))
  }

  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean) = {
    itemRecScoreColl.remove(MongoDBObject("algoid" -> algoid, "modelset" -> modelset))
  }

  def existByAlgo(algo: Algo) = {
    itemRecScoreColl.findOne(MongoDBObject("algoid" -> algo.id, "modelset" -> algo.modelset)) map { _ => true } getOrElse false
  }

  /** Private mapping function to map DB Object to ItemRecScore object */
  private def dbObjToItemRecScore(dbObj: DBObject, appid: Int) = {
    ItemRecScore(
      uid = dbObj.as[String]("uid").drop(appid.toString.length+1),
      iid = dbObj.as[String]("iid").drop(appid.toString.length+1),
      score = dbObj.as[Double]("score"),
      itypes = mongoDbListToListOfString(dbObj.as[MongoDBList]("itypes")),
      appid = appid,
      algoid = dbObj.as[Int]("algoid"),
      modelset = dbObj.as[Boolean]("modelset"),
      id = Some(dbObj.as[ObjectId]("_id"))
    )
  }

  class MongoItemRecScoreIterator(it: MongoCursor, appid: Int) extends Iterator[ItemRecScore] {
    def hasNext = it.hasNext
    def next = dbObjToItemRecScore(it.next, appid)
  }
}
