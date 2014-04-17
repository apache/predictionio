package io.prediction.commons.modeldata

import io.prediction.commons.{ Config }

case class MetadataKeyval(
  algoid: Int,
  modelset: Boolean,
  key: String,
  value: String)

trait MetadataKeyvals extends ModelData {
  def upsert(algoid: Int, modelset: Boolean, key: String, value: String): Unit
  def upsert(keyval: MetadataKeyval): Unit
  def get(algoid: Int, modelset: Boolean, key: String): Option[String]
}
