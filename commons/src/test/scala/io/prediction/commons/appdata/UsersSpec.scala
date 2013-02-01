package io.prediction.commons.appdata

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import org.scala_tools.time.Imports._

class UsersSpec extends Specification { def is =
  "PredictionIO App Data Users Specification"                                 ^
                                                                              p ^
  "Users can be implemented by:"                                              ^ endp ^
    "1. MongoUsers"                                                           ^ mongoUsers ^ end

  def mongoUsers =                                                            p ^
    "MongoUsers should"                                                       ^
      "behave like any Users implementation"                                  ^ users(newMongoUsers) ^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def users(users: Users) = {                                                 t ^
    "inserting and getting a user"                                            ! insert(users) ^
    "updating a user"                                                         ! update(users) ^
    "deleting a user"                                                         ! delete(users) ^
                                                                              bt
  }

  val mongoDbName = "predictionio_appdata_mongousers_test"
  def newMongoUsers = new mongodb.MongoUsers(MongoConnection()(mongoDbName))

  def insert(users: Users) = {
    val appid = 0
    val id = "insert"
    val user = User(
      id = id,
      appid = appid,
      ct = DateTime.now,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    users.insert(user)
    users.get(appid, id) must beSome(user)
  }

  def update(users: Users) = {
    val appid = 1
    val id = "update"
    val user = User(
      id = id,
      appid = appid,
      ct = DateTime.now,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    val updatedUser = user.copy(
      latlng = None,
      inactive = Some(true),
      attributes = Some(Map("dead" -> "beef"))
    )
    users.insert(user)
    users.update(updatedUser)
    users.get(appid, id) must beSome(updatedUser)
  }

  def delete(users: Users) = {
    val appid = 2
    val id = "delete"
    val user = User(
      id = id,
      appid = appid,
      ct = DateTime.now,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    users.delete(user)
    users.get(appid, id) must beNone
  }
}
