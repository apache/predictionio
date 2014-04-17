package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.Config
import io.prediction.commons.MongoUtils
import io.prediction.commons.modeldata.{ MetadataKeyval, MetadataKeyvals }

import com.mongodb.casbah.Imports._

class MongoMetadataKeyvals(cfg: Config, db: MongoDB)
    extends MetadataKeyvals with MongoModelData {
  val config = cfg
  val mongodb = db

  override protected def collectionName(algoid: Int, modelset: Boolean) =
    s"algo_${algoid}_${modelset}_meta"

  def upsert(algoid: Int, modelset: Boolean, key: String, value: String) = {
    val q = MongoDBObject("_id" -> key)
    val obj = MongoDBObject("_id" -> key, "value" -> value)
    db(collectionName(algoid, modelset)).update(q, obj, true)
  }

  def upsert(keyval: MetadataKeyval) = {
    upsert(keyval.algoid, keyval.modelset, keyval.key, keyval.value)
  }

  def get(algoid: Int, modelset: Boolean, key: String): Option[String] = {
    val coll = db(collectionName(algoid, modelset))
    coll.findOne(MongoDBObject("_id" -> key)).map(_.as[String]("value"))
  }
}
