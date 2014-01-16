package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.Config
import io.prediction.commons.modeldata.ModelData

import com.mongodb.casbah.Imports._

trait MongoModelData extends ModelData {
  val config: Config
  val mongodb: MongoDB

  def delete(algoid: Int, modelset: Boolean) = {
    mongodb(collectionName(algoid, modelset)).drop()
  }

  def empty(algoid: Int, modelset: Boolean) = {
    !mongodb.collectionExists(collectionName(algoid, modelset)) ||
      mongodb(collectionName(algoid, modelset)).count() == 0
  }

  override def before(algoid: Int, modelset: Boolean) = {
    if (config.modeldataDbSharding) {
      config.modeldataDbShardKeys map { seqOfKeys =>
        if (seqOfKeys.length != 0) {
          val keysObj = seqOfKeys map { k => MongoDBObject(k -> 1) } reduce { _ ++ _ }
          mongodb.command(MongoDBObject(
            "shardCollection" -> collectionName(algoid, modelset),
            "key" -> keysObj))
        }
      }
    }
  }

  override def after(algoid: Int, modelset: Boolean) = {}

  protected def collectionName(algoid: Int, modelset: Boolean) = s"algo_${algoid}_${modelset}"
}
