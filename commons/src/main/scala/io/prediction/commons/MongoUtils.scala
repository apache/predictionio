package io.prediction.commons

import com.mongodb.casbah.Imports._

object MongoUtils {
  /** Empty MongoDBObject. */
  val emptyObj = MongoDBObject()

  /** Modified from Salat. */
  def dbObjToMap(dbObj: DBObject): Map[String, Any] = {
    val builder = Map.newBuilder[String, Any]

    dbObj foreach {
      case (field, value) =>
        builder += field -> value
    }

    builder.result()
  }

  def dbObjToMapOfString(dbObj: DBObject): Map[String, String] = dbObjToMap(dbObj) mapValues { _.asInstanceOf[String] }

  /** Converts MongoDBList to List of String. */
  def mongoDbListToListOfString(dbList: MongoDBList): List[String] = {
    dbList.toList.map(_.asInstanceOf[String])
  }

  /** Converts BasicDBList to List of String. */
  def basicDBListToListOfString(dbList: BasicDBList): List[String] = {
    dbList.toList.map(_.asInstanceOf[String])
  }

  /** Converts MongoDBList to List of Double. */
  def mongoDbListToListOfDouble(dbList: MongoDBList): List[Double] = {
    dbList.toList.map(_.asInstanceOf[Double])
  }

  def mongoDbListToListofListOfString(dbList: MongoDBList): List[List[String]] = {
    dbList.toList.map(x => basicDBListToListOfString(x.asInstanceOf[BasicDBList]))
  }

  /**
   * Convert custom attributes Map into MongoDBObject and add prefix "ca_"
   *  eg.
   *  from attributes = Map("gender" -> "female", "member" -> "gold")
   *  to MongoDBObject("ca_gender" : "female", "ca_member" : "gold" )
   */
  def attributesToMongoDBObject(attributes: Map[String, Any]) = {
    MongoDBObject((attributes map { case (k, v) => ("ca_" + k, v) }).toList)
  }

  /** Extract custom attributes (with prefix "ca_") from DBObject, strip prefix and create Map[String, Any] */
  def getAttributesFromDBObject(dbObj: DBObject): Map[String, Any] = {
    dbObj.filter { case (k, v) => (k.startsWith("ca_")) }.toList.map { case (k, v) => (k.stripPrefix("ca_"), v) }.toMap
  }

  /**
   * Appends App ID to any ID.
   * Used for distinguishing different app's data within a single collection.
   */
  def idWithAppid(appid: Int, id: String) = appid + "_" + id
}
