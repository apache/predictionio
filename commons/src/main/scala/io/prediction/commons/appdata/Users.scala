package io.prediction.commons.appdata

import com.github.nscala_time.time.Imports._

/**
 * User object.
 *
 * @param id ID.
 * @param appid App ID that this user belongs to.
 * @param ct Creation time.
 * @param latlng Geolocation of this user.
 * @param inactive Whether to disregard this user during any computation.
 * @param attributes Attributes associated with this user.
 */
case class User(
  id: String,
  appid: Int,
  ct: DateTime,
  latlng: Option[Tuple2[Double, Double]],
  inactive: Option[Boolean],
  attributes: Option[Map[String, Any]])

/** Base trait for implementations that interact with users in the backend app data store. */
trait Users {
  /** Insert a new user. */
  def insert(user: User): Unit

  /** Find a user by ID. */
  def get(appid: Int, id: String): Option[User]

  /** Find all users by App ID. */
  def getByAppid(appid: Int): Iterator[User]

  /** Update a user. */
  def update(user: User): Unit

  /** Delete a user. */
  def delete(appid: Int, id: String): Unit

  /** Delete a user. */
  def delete(user: User): Unit

  /** Delete all users by App ID */
  def deleteByAppid(appid: Int): Unit

  /** count number of records by App ID*/
  def countByAppid(appid: Int): Long

}
