package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.Config
import io.prediction.commons.MongoUtils._
import io.prediction.commons.modeldata.{ ItemRecScore, ItemRecScores }
import io.prediction.commons.settings.{ Algo, App, OfflineEval }

import com.mongodb.casbah.Imports._

/** MongoDB implementation of ItemRecScores. */
class MongoItemRecScores(cfg: Config, db: MongoDB) extends ItemRecScores with MongoModelData {
  val config = cfg
  val mongodb = db

  /** Indices and hints. */
  val queryIndex = MongoDBObject("uid" -> 1)

  def getByUid(uid: String)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Option[ItemRecScore] = {
    val modelset = offlineEval map { _ => false } getOrElse algo.modelset
    val itemRecScoreColl = db(collectionName(algo.id, modelset))

    itemRecScoreColl.ensureIndex(queryIndex) // not needed here. it's called in after(), just safety measure in case after() is not called

    itemRecScoreColl.findOne(MongoDBObject("uid" -> idWithAppid(app.id, uid))).map(dbObjToItemRecScore(_, app.id))
  }

  def getTopNIids(uid: String, n: Int, itypes: Option[Seq[String]])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Iterator[String] = {
    val modelset = offlineEval map { _ => false } getOrElse algo.modelset
    val itemRecScoreColl = db(collectionName(algo.id, modelset))

    itemRecScoreColl.ensureIndex(queryIndex) // not needed here. it's called in after(), just safety measure in case after() is not called

    itemRecScoreColl.findOne(MongoDBObject("uid" -> idWithAppid(app.id, uid))).map(dbObjToItemRecScore(_, app.id)).map {
      x: ItemRecScore =>

        val iids = itypes.map { s =>
          val iidsAndItypes = x.iids.zip(x.itypes.map(_.toSet)) // List( (iid, Set(itypes of this iid)), ... )
          val itypesSet: Set[String] = s.toSet // query itypes Set
          val itypesSetSize = itypesSet.size

          iidsAndItypes.filter {
            case (iid, iiditypes) =>
              // if there are some elements in s existing in iiditypes, then s.diff(iiditypes) size will be < original size of s
              // it means itypes match the item
              (itypesSet.diff(iiditypes).size < itypesSetSize)
          }.map(_._1) // only return the iid
        }.getOrElse {
          x.iids
        }

        val topNIids = if (n == 0) iids else iids.take(n)

        topNIids
    }.getOrElse(Seq()).toIterator
  }

  def insert(itemrecscore: ItemRecScore) = {
    val id = new ObjectId
    val itemRecObj = MongoDBObject(
      "_id" -> id,
      "uid" -> idWithAppid(itemrecscore.appid, itemrecscore.uid),
      "iids" -> itemrecscore.iids.map(i => idWithAppid(itemrecscore.appid, i)),
      "scores" -> itemrecscore.scores,
      "itypes" -> itemrecscore.itypes,
      "algoid" -> itemrecscore.algoid,
      "modelset" -> itemrecscore.modelset
    )
    db(collectionName(itemrecscore.algoid, itemrecscore.modelset)).insert(itemRecObj)
    itemrecscore.copy(id = Some(id))
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

  /** Private mapping function to map DB Object to ItemRecScore object */
  private def dbObjToItemRecScore(dbObj: DBObject, appid: Int) = {
    ItemRecScore(
      uid = dbObj.as[String]("uid").drop(appid.toString.length + 1),
      iids = mongoDbListToListOfString(dbObj.as[MongoDBList]("iids")).map(_.drop(appid.toString.length + 1)),
      scores = mongoDbListToListOfDouble(dbObj.as[MongoDBList]("scores")),
      itypes = mongoDbListToListofListOfString(dbObj.as[MongoDBList]("itypes")),
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
