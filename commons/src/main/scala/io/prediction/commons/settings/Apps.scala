package io.prediction.commons.settings

/** App object.
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
  url: Option[String],
  cat: Option[String],
  desc: Option[String],
  timezone: String
)

/** Base trait for implementations that interact with apps in the backend data store. */
trait Apps {
  /** Insert a new App with basic fields defined.
    *
    * @param app An App object to be inserted. The ID will be ignored and replaced by an implementation of this trait.
    */
  def insert(app: App): Int

  /** Get an App by its ID. */
  def get(id: Int): Option[App]

  /** Get Apps by user ID. */
  def getByUserid(userid: Int): Iterator[App]

  /** Get an App by its appkey. */
  def getByAppkey(appkey: String): Option[App]

  /** Get an App by its appkey and user ID. */
  def getByAppkeyAndUserid(appkey: String, userid: Int): Option[App]

  /** Get an App by its ID and user ID. */
  def getByIdAndUserid(id: Int, userid: Int): Option[App]

  /** Update app information. */
  def update(app: App)

  /** Update app's appkey by its appkey and user ID. */
  def updateAppkeyByAppkeyAndUserid(appkey: String, userid: Int, newAppkey: String): Option[App]

  /** Update app's timezone by its appkey and user ID. */
  def updateTimezoneByAppkeyAndUserid(appkey: String, userid: Int, timezone: String): Option[App]

  /** Delete an App by ID and user ID. */
  def deleteByIdAndUserid(id: Int, userid: Int)

  /** Check if this app exists by its ID, appkey and user ID.
    *
    * For purpose of making sure this app exists and belongs to the specified
    * user ID.
    */
  def existsByIdAndAppkeyAndUserid(id: Int, appkey: String, userid: Int): Boolean
}