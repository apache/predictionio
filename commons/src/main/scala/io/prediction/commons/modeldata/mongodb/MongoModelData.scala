package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.modeldata.ModelData

import com.mongodb.casbah.Imports._

trait MongoModelData extends ModelData {
  val mongodb: MongoDB

  def delete(algoid: Int, modelset: Boolean) = {
    mongodb(collectionName(algoid, modelset)).drop()
  }

  def empty(algoid: Int, modelset: Boolean) = {
    !mongodb.collectionExists(collectionName(algoid, modelset)) ||
      mongodb(collectionName(algoid, modelset)).count() == 0
  }

  override def before(algoid: Int, modelset: Boolean) = Unit

  override def after(algoid: Int, modelset: Boolean) = Unit

  private def collectionName(algoid: Int, modelset: Boolean) = s"algo_${algoid}_${modelset}"
}
