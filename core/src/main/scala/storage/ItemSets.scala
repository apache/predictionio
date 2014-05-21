package io.prediction.storage

import com.github.nscala_time.time.Imports._

case class ItemSet(
  id: String,
  appid: Int,
  iids: Seq[String],
  t: Option[DateTime])

trait ItemSets {
  /** Insert new ItemSet */
  def insert(itemSet: ItemSet): Unit

  /** Get an item set */
  def get(appid: Int, id: String): Option[ItemSet]

  /** Get by appid */
  def getByAppid(appid: Int): Iterator[ItemSet]

  /** Delete itemSet */
  def delete(itemSet: ItemSet): Unit

  /** Delete by appid */
  def deleteByAppid(appid: Int): Unit

}
