package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{ User, Users }

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.WriteConcern

/** MongoDB implementation of Users. */
class MongoUsers(db: MongoDB) extends Users {
  private val emptyObj = MongoDBObject()
  private val userColl = db("users")
  private val seq = new MongoSequences(db)

  userColl.setWriteConcern(WriteConcern.JournalSafe)

  def authenticate(id: Int, password: String) = {
    userColl.findOne(MongoDBObject("_id" -> id, "password" -> password) ++ ("confirm" $exists false)) map { _ => true } getOrElse false
  }

  def authenticateByEmail(email: String, password: String) = {
    userColl.findOne(MongoDBObject("email" -> email, "password" -> password) ++ ("confirm" $exists false)) map { _.as[Int]("_id") }
  }

  def insert(email: String, password: String, firstname: String, lastname: Option[String], confirm: String) = {
    val id = seq.genNext("userid")
    val userObj = MongoDBObject(
      "_id" -> id,
      "email" -> email,
      "password" -> password,
      "firstname" -> firstname,
      "confirm" -> confirm)
    val lastnameObj = lastname.map(ln => MongoDBObject("lastname" -> ln)).getOrElse(emptyObj)
    userColl.save(userObj ++ lastnameObj)
    id
  }

  def get(id: Int) = {
    userColl.findOne(MongoDBObject("_id" -> id)) map { dbObjToUser(_) }
  }

  def getAll() = new MongoUserIterator(userColl.find())

  def getByEmail(email: String) = {
    userColl.findOne(MongoDBObject("email" -> email)) map { dbObjToUser(_) }
  }

  def update(user: User, upsert: Boolean = false) = {
    val requiredObj = MongoDBObject(
      "_id" -> user.id,
      "email" -> user.email,
      "password" -> user.password,
      "firstname" -> user.firstName)
    val lastnameObj = user.lastName map { x => MongoDBObject("lastname" -> x) } getOrElse { MongoUtils.emptyObj }
    val confirmObj = user.confirm map { x => MongoDBObject("confirm" -> x) } getOrElse { MongoUtils.emptyObj }
    userColl.update(MongoDBObject("_id" -> user.id), requiredObj ++ lastnameObj ++ confirmObj, upsert)
  }

  def updateEmail(id: Int, email: String) = {
    userColl.update(MongoDBObject("_id" -> id), MongoDBObject("$set" -> MongoDBObject("email" -> email)))
  }

  def updatePassword(userid: Int, password: String) = {
    userColl.update(MongoDBObject("_id" -> userid), MongoDBObject("$set" -> MongoDBObject("password" -> password)))
  }

  def updatePasswordByEmail(email: String, password: String) = {
    userColl.update(MongoDBObject("email" -> email), MongoDBObject("$set" -> MongoDBObject("password" -> password)))
  }

  def confirm(confirm: String) = {
    userColl.findAndModify(MongoDBObject("confirm" -> confirm), MongoDBObject("$unset" -> MongoDBObject("confirm" -> 1))) map { dbObjToUser(_) }
  }

  def emailExists(email: String) = {
    userColl.findOne(MongoDBObject("email" -> email)).map(_ => true).getOrElse(false)
  }

  def idAndEmailExists(userid: Int, email: String) = {
    userColl.findOne(MongoDBObject("_id" -> userid, "email" -> email)).map(_ => true).getOrElse(false)
  }

  private def dbObjToUser(dbObj: DBObject): User = {
    User(
      id = dbObj.as[Int]("_id"),
      firstName = dbObj.as[String]("firstname"),
      lastName = dbObj.getAs[String]("lastname"),
      email = dbObj.as[String]("email"),
      password = dbObj.as[String]("password"),
      confirm = dbObj.getAs[String]("confirm")
    )
  }

  class MongoUserIterator(it: MongoCursor) extends Iterator[User] {
    def next = dbObjToUser(it.next)
    def hasNext = it.hasNext
  }
}