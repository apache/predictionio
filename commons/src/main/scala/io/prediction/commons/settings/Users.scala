package io.prediction.commons.settings

/** User object.
  *
  * @param id ID.
  * @param firstName First name.
  * @param lastName Last name.
  * @param email E-mail.
  */
case class User(
  id: Int,
  firstName: String,
  lastName: Option[String],
  email: String
)

/** Base trait for implementations that interact with users in the backend data store. */
trait Users {
  /** Authenticate a user by ID and password. */
  def authenticate(id: Int, password: String): Boolean

  /** Authenticate a user by e-mail and password. */
  def authenticateByEmail(email: String, password: String): Option[Int]

  /** Inserts a new user. */
  def insert(email: String, password: String, firstname: String, lastname: Option[String], confirm: String): Int

  /** Finds a user by ID. */
  def get(id: Int): Option[User]

  /** Finds all users. */
  def getAll(): Iterator[User]

  /** Finds a user by e-mail. */
  def getByEmail(email: String): Option[User]

  /** Update a user. */
  def update(user: User)

  /** Update email address by ID. */
  def updateEmail(id: Int, email: String)

  /** Update password by ID. */
  def updatePassword(id: Int, password: String)

  /** Update password by e-mail.
    *
    * Note: For reset password requests.
    */
  def updatePasswordByEmail(email: String, password: String)

  /** Confirms a new user. */
  def confirm(confirm: String): Option[User]

  /** Check if an e-mail address exists. */
  def emailExists(email: String): Boolean

  /** Check if an ID and e-mail combination exists. */
  def idAndEmailExists(userid: Int, email: String): Boolean
}