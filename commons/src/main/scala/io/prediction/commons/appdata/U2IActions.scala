package io.prediction.commons.appdata

import com.github.nscala_time.time.Imports._

/**
 * User-to-item action object.
 *
 * @param appid App ID that this item belongs to.
 * @param action Type of this action.
 * @param uid User ID of this action.
 * @param iid Item ID of this action.
 * @param t Time of this action.
 * @param latlng Geolocation of this action.
 * @param v The value of this action (if applicable).
 * @param price Price associated with this action (if applicable).
 */
case class U2IAction(
  appid: Int,
  action: String,
  uid: String,
  iid: String,
  t: DateTime,
  latlng: Option[Tuple2[Double, Double]],
  v: Option[Int],
  price: Option[Double])

/** Base trait for implementations that interact with user-to-item actions in the backend app data store. */
trait U2IActions {
  /** Represents a user-rate-item action. */
  val rate = "rate"

  /** Represents a user-like/dislike-item action. */
  val like = "like"

  val dislike = "dislike"

  /** Represents a user-view-item action. */
  val view = "view"

  /** Represents a user-view-item's details action. */
  val viewDetails = "viewDetails"

  /** Represents a user-item conversion (e.g. buy) action. */
  val conversion = "conversion"

  /** Inserts a user-to-item action. */
  def insert(u2iAction: U2IAction): Unit

  /** Gets all user-to-item actions by App ID. */
  def getAllByAppid(appid: Int): Iterator[U2IAction]

  /** Gets all user-to-item actions by App ID, User ID, and Item IDs. */
  def getAllByAppidAndUidAndIids(appid: Int, uid: String, iids: Seq[String]): Iterator[U2IAction]

  /** Get all users-to-item actions by AppID, Item ID and optionally sort by User ID */
  def getAllByAppidAndIid(appid: Int, iid: String, sortedByUid: Boolean = true): Iterator[U2IAction]

  /** Delete all user-to-item actions by App ID */
  def deleteByAppid(appid: Int): Unit

  /** count number of records by App ID*/
  def countByAppid(appid: Int): Long
}
