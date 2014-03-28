package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class UsersSpec extends Specification {
  def is = s2"""

  PredictionIO Users Specification

    Users can be implemented by:
    - MongoUsers $mongoUsers

  """

  def mongoUsers = s2"""

    MongoUsers should
    - behave like any Users implementation ${users(newMongoUsers)}
    - (database cleanup) ${Step(MongoConnection()(mongoDbName).dropDatabase())}

  """

  def users(users: Users) = s2"""

    inserting a user ${insert(users)}
    looking up an existing e-mail ${emailExists(users)}
    looking up a non-existing e-mail should fail ${emailExistsNonExist(users)}
    looking up an existing ID and e-mail combo ${idAndEmailExists(users)}
    looking up a non-existing ID and e-mail combo should fail ${idAndEmailExistsNonExist(users)}
    getting a user by ID ${get(users)}
    getting a user by e-mail ${getByEmail(users)}
    authenticating a non-existing user and fail ${authenticateNonExist(users)}
    authenticating an unconfirmed user and fail ${authenticateUnconfirmed(users)}
    authenticating a confirmed user ${authenticate(users)}
    authenticating by e-mail a non-existing user and fail ${authenticateByEmailNonExist(users)}
    authenticating by e-mail an unconfirmed user and fail ${authenticateByEmailUnconfirmed(users)}
    authenticating by e-mail a confirmed user ${authenticateByEmail(users)}
    confirming a non-existing user and fail ${confirmNonExist(users)}
    confirming an unconfirmed user ${confirm(users)}
    updating a user's e-mail by ID ${updateEmail(users)}
    updating a user's password by ID ${updatePassword(users)}
    updating a user's password by e-mail ${updatePasswordByEmail(users)}
    updating a user ${update(users)}
    backup and restore users ${backuprestore(users)}

  """

  val mongoDbName = "predictionio_mongousers_test"
  def newMongoUsers = new mongodb.MongoUsers(MongoConnection()(mongoDbName))

  def insert(users: Users) = {
    val name = "insert"
    users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = Option(name),
      confirm = name
    )
    users.emailExists(name + "@prediction.io") must beTrue
  }

  def get(users: Users) = {
    val name = "get"
    val id = users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = Option(name),
      confirm = name
    )
    users.get(id) must beSome(User(id, name, Option(name), name + "@prediction.io", name, Some(name)))
  }

  def getByEmail(users: Users) = {
    val name = "getByEmail"
    val id = users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = None,
      confirm = name
    )
    users.getByEmail(name + "@prediction.io") must beSome(User(id, name, None, name + "@prediction.io", name, Some(name)))
  }

  def emailExists(users: Users) = {
    val dummy = "emailExists"
    val email = dummy + "@prediction.io"
    users.insert(
      email = email,
      password = dummy,
      firstname = dummy,
      lastname = None,
      confirm = dummy
    )
    users.emailExists(email) must beTrue
  }

  def emailExistsNonExist(users: Users) = {
    users.emailExists("nonexist@prediction.io") must beFalse
  }

  def idAndEmailExists(users: Users) = {
    val name = "idAndEmailExists"
    val id = users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = Option(name),
      confirm = name
    )
    users.idAndEmailExists(id, name + "@prediction.io") must beTrue
  }

  def idAndEmailExistsNonExist(users: Users) = {
    users.idAndEmailExists(4789, "nonexist@prediction.io") must beFalse
  }

  def authenticateNonExist(users: Users) = {
    users.authenticate(342, "bar") must beFalse
  }

  def authenticateUnconfirmed(users: Users) = {
    val name = "authenticateUnconfirmed"
    val id = users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = Option(name),
      confirm = name
    )
    users.authenticate(id, name) must beFalse
  }

  def authenticate(users: Users) = {
    val name = "authenticate"
    val id = users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = Option(name),
      confirm = name
    )
    users.confirm(name)
    users.authenticate(id, name) must beTrue
  }

  def authenticateByEmailNonExist(users: Users) = {
    users.authenticateByEmail("foo", "bar") must beNone
  }

  def authenticateByEmailUnconfirmed(users: Users) = {
    val name = "authenticateByEmailUnconfirmed"
    users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = Option(name),
      confirm = name
    )
    users.authenticateByEmail(name + "@predictionio", name) must beNone
  }

  def authenticateByEmail(users: Users) = {
    val name = "authenticateByEmail"
    val id = users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = Option(name),
      confirm = name
    )
    users.confirm(name)
    users.authenticateByEmail(name + "@prediction.io", name) must beSome(id)
  }

  def confirmNonExist(users: Users) = {
    users.confirm("fakecode") must beNone
  }

  def confirm(users: Users) = {
    val name = "confirm"
    val id = users.insert(
      email = name + "@prediction.io",
      password = name,
      firstname = name,
      lastname = None,
      confirm = name
    )
    users.confirm(name) must beSome(User(id, name, None, name + "@prediction.io", name, Some(name)))
  }

  def updateEmail(users: Users) = {
    val id = users.insert(
      email = "updateEmail@prediction.io",
      password = "updateEmail",
      firstname = "updateEmail",
      lastname = None,
      confirm = "updateEmail"
    )
    users.updateEmail(id, "updateEmailUpdated@prediction.io")
    users.get(id) must beSome(User(id, "updateEmail", None, "updateEmailUpdated@prediction.io", "updateEmail", Some("updateEmail")))
  }

  def updatePassword(users: Users) = {
    val id = users.insert(
      email = "updatePassword@prediction.io",
      password = "updatePassword",
      firstname = "updatePassword",
      lastname = None,
      confirm = "updatePassword"
    )
    users.confirm("updatePassword")
    users.updatePassword(id, "updatePasswordUpdated")
    users.authenticate(id, "updatePasswordUpdated") must beTrue
  }

  def updatePasswordByEmail(users: Users) = {
    val id = users.insert(
      email = "updatePasswordByEmail@prediction.io",
      password = "updatePasswordByEmail",
      firstname = "updatePasswordByEmail",
      lastname = None,
      confirm = "updatePasswordByEmail"
    )
    users.confirm("updatePasswordByEmail")
    users.updatePasswordByEmail("updatePasswordByEmail@prediction.io", "updatePasswordByEmailUpdated")
    users.authenticate(id, "updatePasswordByEmailUpdated") must beTrue
  }

  def update(users: Users) = {
    val id = users.insert(
      email = "update@prediction.io",
      password = "update",
      firstname = "update",
      lastname = None,
      confirm = "update"
    )
    val updatedUser = User(id, "updated", None, "updated@prediction.io")
    users.update(updatedUser)
    users.get(id) must beSome(updatedUser)
  }

  def backuprestore(users: Users) = {
    val user1 = User(0, "english中文", None, "backuprestore@prediction.io", "password", Some("backuprestore"))
    val id1 = users.insert(
      email = user1.email,
      password = user1.password,
      firstname = user1.firstName,
      lastname = user1.lastName,
      confirm = user1.confirm.get
    )
    val fn = "users.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(users.backup())
    } finally {
      fos.close()
    }
    users.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { data =>
      data must contain(user1.copy(id = id1))
    } getOrElse 1 === 2
  }
}
