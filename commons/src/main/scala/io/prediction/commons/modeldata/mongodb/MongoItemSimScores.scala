package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.Config
import io.prediction.commons.MongoUtils._
import io.prediction.commons.modeldata.{ ItemSimScore, ItemSimScores }
import io.prediction.commons.settings.{ Algo, App, OfflineEval }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of ItemSimScores. */
class MongoItemSimScores(cfg: Config, db: MongoDB) extends ItemSimScores with MongoModelData {
  val config = cfg
  val mongodb = db

  /** Indices and hints. */
  val queryIndex = MongoDBObject("iid" -> 1)

  def getByIid(iid: String)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Option[ItemSimScore] = {
    val modelset = offlineEval map { _ => false } getOrElse algo.modelset
    val itemSimScoreColl = db(collectionName(algo.id, modelset))

    itemSimScoreColl.ensureIndex(queryIndex) // not needed here. it's called in after(), just safety measure in case after() is not called

    itemSimScoreColl.findOne(MongoDBObject("iid" -> idWithAppid(app.id, iid))).map(dbObjToItemSimScore(_, app.id))
  }

  def getTopNIids(iid: String, n: Int, itypes: Option[Seq[String]])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Iterator[String] = {
    val modelset = offlineEval map { _ => false } getOrElse algo.modelset
    val itemSimScoreColl = db(collectionName(algo.id, modelset))

    itemSimScoreColl.ensureIndex(queryIndex) // not needed here. it's called in after(), just safety measure in case after() is not called

    itemSimScoreColl.findOne(MongoDBObject("iid" -> idWithAppid(app.id, iid))).map(dbObjToItemSimScore(_, app.id)).map {
      x: ItemSimScore =>

        val simiids = itypes.map { s =>
          val simiidsAndItypes = x.simiids.zip(x.itypes.map(_.toSet)) // List( (iid, Set(itypes of this iid)), ... )
          val itypesSet: Set[String] = s.toSet // query itypes Set
          val itypesSetSize = itypesSet.size

          simiidsAndItypes.filter {
            case (simiid, iiditypes) =>
              // if there are some elements in s existing in iiditypes, then s.diff(iiditypes) size will be < original size of s
              // it means itypes match the item
              (itypesSet.diff(iiditypes).size < itypesSetSize)
          }.map(_._1) // only return the iid
        }.getOrElse {
          x.simiids
        }

        val topNIids = if (n == 0) simiids else simiids.take(n)

        topNIids
    }.getOrElse(Seq()).toIterator

  }

  def insert(itemSimScore: ItemSimScore) = {
    val id = new ObjectId
    val itemSimObj = MongoDBObject(
      "_id" -> id,
      "iid" -> idWithAppid(itemSimScore.appid, itemSimScore.iid),
      "simiids" -> itemSimScore.simiids.map(i => idWithAppid(itemSimScore.appid, i)),
      "scores" -> itemSimScore.scores,
      "simitypes" -> itemSimScore.itypes,
      "algoid" -> itemSimScore.algoid,
      "modelset" -> itemSimScore.modelset
    )
    db(collectionName(itemSimScore.algoid, itemSimScore.modelset)).insert(itemSimObj)
    itemSimScore.copy(id = Some(id))
  }

  def deleteByAlgoid(algoid: Int) = {
    db(collectionName(algoid, true)).drop()
    db(collectionName(algoid, false)).drop()
  }

  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean) = {
    db(collectionName(algoid, modelset)).drop()
  }

  def existByAlgo(algo: Algo) = {
    db.collectionExists(collectionName(algo.id, algo.modelset))
  }

  override def after(algoid: Int, modelset: Boolean) = {
    val coll = db(collectionName(algoid, modelset))

    coll.ensureIndex(queryIndex)
  }

  /** Private mapping function to map DB Object to ItemSimScore object */
  private def dbObjToItemSimScore(dbObj: DBObject, appid: Int) = {
    ItemSimScore(
      iid = dbObj.as[String]("iid").drop(appid.toString.length + 1),
      simiids = mongoDbListToListOfString(dbObj.as[MongoDBList]("simiids")).map(_.drop(appid.toString.length + 1)),
      scores = mongoDbListToListOfDouble(dbObj.as[MongoDBList]("scores")),
      itypes = mongoDbListToListofListOfString(dbObj.as[MongoDBList]("simitypes")),
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
