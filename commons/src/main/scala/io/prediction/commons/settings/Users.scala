package io.prediction.commons.settings

import com.twitter.chill.KryoInjection

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
  email: String,
  password: String = "",
  confirm: Option[String] = None)

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

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = {
    val backup = getAll().toSeq.map { b =>
      Map(
        "id" -> b.id,
        "firstName" -> b.firstName,
        "lastName" -> b.lastName,
        "email" -> b.email,
        "password" -> b.password,
        "confirm" -> b.confirm)
    }
    KryoInjection(backup)
  }

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], upgrade: Boolean = false): Option[Seq[User]] = {
    KryoInjection.invert(bytes) map { r =>
      r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        User(
          id = data("id").asInstanceOf[Int],
          firstName = data("firstName").asInstanceOf[String],
          lastName = data("lastName").asInstanceOf[Option[String]],
          email = data("email").asInstanceOf[String],
          password = data("password").asInstanceOf[String],
          confirm = data("confirm").asInstanceOf[Option[String]])
      }
    }
  }
}
