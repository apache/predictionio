package io.prediction.commons.appdata

import org.scala_tools.time.Imports._

/** Item object.
  *
  * @param id ID.
  * @param appid App ID that this item belongs to.
  * @param ct Creation time.
  * @param itypes Item types.
  * @param startt The start time when this item becomes valid.
  * @param endt The end time when this item becomes invalid.
  * @param price Price of this item.
  * @param profit Net profit made by this item.
  * @param latlng Geolocation of this item.
  * @param inactive Whether to disregard this item during any computation.
  * @param attributes Attributes associated with this item.
  */
case class Item(
  id: String,
  appid: Int,
  ct: DateTime,
  itypes: List[String],
  startt: Option[DateTime],
  endt: Option[DateTime],
  price: Option[Double],
  profit: Option[Double],
  latlng: Option[Tuple2[Double, Double]],
  inactive: Option[Boolean],
  attributes: Option[Map[String, Any]]
)

/** Base trait for implementations that interact with items in the backend app data store. */
trait Items {
  /** Inserts a new item. */
  def insert(item: Item): Unit

  /** Finds an item by ID. */
  def get(appid: Int, id: String): Option[Item]

  /** Update an item. */
  def update(item: Item): Unit

  /** Delete an item. */
  def delete(appid: Int, id: String): Unit

  /** Delete an item. */
  def delete(item: Item): Unit
  
  /** Delete all items by App ID */
  def deleteByAppid(appid: Int): Unit
  
}
