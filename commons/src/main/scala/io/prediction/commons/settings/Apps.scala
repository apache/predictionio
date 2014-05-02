package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * App object.
 *
 * @param id ID.
 * @param userid User ID that owns this app.
 * @param appkey The appkey used to access this app via REST API.
 * @param display The app's display name.
 * @param url The URL where the app is used.
 * @param cat The app's category.
 * @param desc The app's description.
 * @param timezone The app's timezone.
 */
case class App(
  id: Int,
  userid: Int,
  appkey: String,
  display: String,
  url: Option[String] = Some(""),
  cat: Option[String] = Some(""),
  desc: Option[String] = Some(""),
  timezone: String = "UTC")

/** Base trait for implementations that interact with apps in the backend data store. */
trait Apps extends Common {
  /**
   * Insert a new App with basic fields defined.
   *
   * @param app An App object to be inserted. The ID will be ignored and replaced by an implementation of this trait.
   */
  def insert(app: App): Int

  /** Get an App by its ID. */
  def get(id: Int): Option[App]

  /** Get all Apps. */
  def getAll(): Iterator[App]

  /** Get Apps by user ID. */
  def getByUserid(userid: Int): Iterator[App]

  /** Get an App by its appkey. */
  def getByAppkey(appkey: String): Option[App]

  /** Get an App by its appkey and user ID. */
  def getByAppkeyAndUserid(appkey: String, userid: Int): Option[App]

  /** Get an App by its ID and user ID. */
  def getByIdAndUserid(id: Int, userid: Int): Option[App]

  /** Update app information. */
  def update(app: App, upsert: Boolean = false)

  /** Update app's appkey by its appkey and user ID. */
  def updateAppkeyByAppkeyAndUserid(appkey: String, userid: Int, newAppkey: String): Option[App]

  /** Update app's timezone by its appkey and user ID. */
  def updateTimezoneByAppkeyAndUserid(appkey: String, userid: Int, timezone: String): Option[App]

  /** Delete an App by ID and user ID. */
  def deleteByIdAndUserid(id: Int, userid: Int)

  /**
   * Check if this app exists by its ID, appkey and user ID.
   *
   * For purpose of making sure this app exists and belongs to the specified
   * user ID.
   */
  def existsByIdAndAppkeyAndUserid(id: Int, appkey: String, userid: Int): Boolean

  implicit val formats = Serialization.formats(NoTypeHints)

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[App]] = {
    try {
      val rdata = Serialization.read[Seq[App]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}
