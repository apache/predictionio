package io.prediction.storage

import com.github.nscala_time.time.Imports._

/**
 * Item object.
 *
 * @param id ID.
 * @param appid App ID that this item belongs to.
 * @param ct Creation time.
 * @param itypes Item types.
 * @param starttime The start time when this item becomes valid.
 * @param endtime The end time when this item becomes invalid.
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
  itypes: Seq[String],
  starttime: Option[DateTime],
  endtime: Option[DateTime],
  price: Option[Double] = None,
  profit: Option[Double] = None,
  latlng: Option[Tuple2[Double, Double]] = None,
  inactive: Option[Boolean] = None,
  attributes: Option[Map[String, Any]] = None)

/**
 * Base trait for implementations that interact with items in the backend app
 * data store.
 */
trait Items {
  /** Insert a new item. */
  def insert(item: Item): Unit

  /** Get an item by ID. */
  def get(appid: Int, id: String): Option[Item]

  /** Find all items by App ID. */
  def getByAppid(appid: Int): Iterator[Item]

  /** Find items by App ID sorted by geolocation distance. */
  def getByAppidAndLatlng(appid: Int, latlng: Tuple2[Double, Double],
    within: Option[Double], unit: Option[String]): Iterator[Item]

  /** Find items by App ID which belong to one of the itypes. */
  def getByAppidAndItypes(appid: Int, itypes: Seq[String]): Iterator[Item]

  /** Find current items by App ID which belong to one of the itypes. */
  def getByAppidAndItypesAndTime(appid: Int,
    optItypes: Option[Seq[String]] = None,
    optTime: Option[DateTime] = None): Iterator[Item]

  /**
   * Get items by IDs. Items returned are not guaranteed to be in the same order
   * as the input, as some IDs may not be valid.
   */
  def getByIds(appid: Int, ids: Seq[String]): Seq[Item]

  /** Get items by IDs sorted by their start time in descending order. */
  def getRecentByIds(appid: Int, ids: Seq[String]): Seq[Item]

  /** Update an item. */
  def update(item: Item): Unit

  /** Delete an item. */
  def delete(appid: Int, id: String): Unit

  /** Delete an item. */
  def delete(item: Item): Unit

  /** Delete all items by App ID */
  def deleteByAppid(appid: Int): Unit

  /** count number of records by App ID*/
  def countByAppid(appid: Int): Long
}
