package io.prediction.commons.settings.mongodb

import io.prediction.commons.settings.{User, Users}
import com.mongodb.casbah.Imports._
import org.apache.commons.codec.digest.DigestUtils

/** MongoDB implementation of Users. */
class MongoUsers(db: MongoDB) extends Users {
  private val emptyObj = MongoDBObject()
  private val userColl = db("users")
  private val seq = new MongoSequences(db)

  private def md5password(password: String) = DigestUtils.md5Hex(password)

  def authenticate(id: Int, password: String) = {
    userColl.findOne(MongoDBObject("_id" -> id, "password" -> md5password(password)) ++ ("confirm" $exists false)) map { _ => true } getOrElse false
  }

  def authenticateByEmail(email: String, password: String) = {
    userColl.findOne(MongoDBObject("email" -> email, "password" -> md5password(password)) ++ ("confirm" $exists false)) map { _.as[Int]("_id") }
  }

  def insert(email: String, password: String, firstname: String, lastname: Option[String], confirm: String) = {
    val id = seq.genNext("userid")
    val userObj = MongoDBObject(
      "_id" -> id,
      "email" -> email,
      "password" -> md5password(password),
      "firstname" -> firstname,
      "confirm" -> confirm)
    val lastnameObj = lastname.map(ln => MongoDBObject("lastname" -> ln)).getOrElse(emptyObj)
    userColl.save(userObj ++ lastnameObj)
    id
  }

  private val getFields = MongoDBObject("firstname" -> 1, "lastname" -> 1, "email" -> 1)

  def get(id: Int) = {
    getByResult(userColl.findOne(MongoDBObject("_id" -> id), getFields))
  }

  def getByEmail(email: String) = {
    getByResult(userColl.findOne(MongoDBObject("email" -> email), getFields))
  }

  private def getByResult(result: Option[DBObject]) = {
    result map { dbObjToUser(_) }
  }

  def update(user: User) = {
    val setFirstname  = MongoDBObject("firstname" -> user.firstName)
    val setLastname   = user.lastName.map { ln => MongoDBObject("lastname" -> ln) } getOrElse(emptyObj)
    val unsetLastname = user.lastName.map { _ => emptyObj } getOrElse MongoDBObject("lastname" -> 1)
    val setEmail      = MongoDBObject("email" -> user.email)

    val setObj = setFirstname ++ setLastname ++ setEmail
    val unsetObj = unsetLastname

    val modObj = MongoDBObject("$set" -> setObj) ++ MongoDBObject("$unset" -> unsetObj)

    userColl.update(MongoDBObject("_id" -> user.id), modObj)
  }

  def updateEmail(id: Int, email: String) = {
    userColl.update(MongoDBObject("_id" -> id), MongoDBObject("$set" -> MongoDBObject("email" -> email)))
  }

  def updatePassword(userid: Int, password: String) = {
    userColl.update(MongoDBObject("_id" -> userid), MongoDBObject("$set" -> MongoDBObject("password" -> md5password(password))))
  }

  def updatePasswordByEmail(email: String, password: String) = {
    userColl.update(MongoDBObject("email" -> email), MongoDBObject("$set" -> MongoDBObject("password" -> md5password(password))))
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

  private def dbObjToUser(dbObj: DBObject) = {
    User(
      id        = dbObj.as[Int]("_id"),
      firstName = dbObj.as[String]("firstname"),
      lastName  = dbObj.getAs[String]("lastname"),
      email     = dbObj.as[String]("email")
    )
  }
}