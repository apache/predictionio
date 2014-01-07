package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.MongoUtils._
import io.prediction.commons.modeldata.{ ItemSimScore, ItemSimScores }
import io.prediction.commons.settings.{ Algo, App, OfflineEval }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of ItemSimScores. */
class MongoItemSimScores(db: MongoDB) extends ItemSimScores with MongoModelData {
  private val itemSimScoreColl = db("itemSimScores")
  val mongodb = db

  /** Indices and hints. */
  val scoreIdIndex = MongoDBObject("score" -> -1, "_id" -> 1)
  itemSimScoreColl.ensureIndex(scoreIdIndex)
  itemSimScoreColl.ensureIndex(MongoDBObject("algoid" -> 1, "iid" -> 1, "modelset" -> 1))

  def getTopN(iid: String, n: Int, itypes: Option[Seq[String]], after: Option[ItemSimScore])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None) = {
    val modelset = offlineEval map { _ => false } getOrElse algo.modelset
    val query = MongoDBObject("algoid" -> algo.id, "iid" -> idWithAppid(app.id, iid), "modelset" -> modelset) ++
      (itypes map { loi => MongoDBObject("simitypes" -> MongoDBObject("$in" -> loi)) } getOrElse emptyObj)
    after map { iss =>
      new MongoItemSimScoreIterator(
        itemSimScoreColl.find(query).
          $min(MongoDBObject("score" -> iss.score, "_id" -> iss.id)).
          sort(scoreIdIndex).
          skip(1).limit(n),
        app.id
      )
    } getOrElse new MongoItemSimScoreIterator(
      itemSimScoreColl.find(query).sort(scoreIdIndex).limit(n),
      app.id
    )
  }

  def insert(itemSimScore: ItemSimScore) = {
    val id = new ObjectId
    val itemSimObj = MongoDBObject(
      "_id" -> id,
      "iid" -> idWithAppid(itemSimScore.appid, itemSimScore.iid),
      "simiid" -> idWithAppid(itemSimScore.appid, itemSimScore.simiid),
      "score" -> itemSimScore.score,
      "simitypes" -> itemSimScore.itypes,
      "algoid" -> itemSimScore.algoid,
      "modelset" -> itemSimScore.modelset
    )
    itemSimScoreColl.insert(itemSimObj)
    itemSimScore.copy(id = Some(id))
  }

  def deleteByAlgoid(algoid: Int) = {
    itemSimScoreColl.remove(MongoDBObject("algoid" -> algoid))
  }

  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean) = {
    itemSimScoreColl.remove(MongoDBObject("algoid" -> algoid, "modelset" -> modelset))
  }

  def existByAlgo(algo: Algo) = {
    itemSimScoreColl.findOne(MongoDBObject("algoid" -> algo.id, "modelset" -> algo.modelset)) map { _ => true } getOrElse false
  }

  /** Private mapping function to map DB Object to ItemSimScore object */
  private def dbObjToItemSimScore(dbObj: DBObject, appid: Int) = {
    ItemSimScore(
      iid = dbObj.as[String]("iid").drop(appid.toString.length + 1),
      simiid = dbObj.as[String]("simiid").drop(appid.toString.length + 1),
      score = dbObj.as[Double]("score"),
      itypes = mongoDbListToListOfString(dbObj.as[MongoDBList]("simitypes")),
      appid = appid,
      algoid = dbObj.as[Int]("algoid"),
      modelset = dbObj.as[Boolean]("modelset"),
      id = Some(dbObj.as[ObjectId]("_id"))
    )
  }

  class MongoItemSimScoreIterator(it: MongoCursor, appid: Int) extends Iterator[ItemSimScore] {
    def hasNext = it.hasNext
    def next = dbObjToItemSimScore(it.next, appid)
  }
}
