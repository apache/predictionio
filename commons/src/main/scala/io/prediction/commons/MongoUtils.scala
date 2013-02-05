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

  /** Converts MongoDBList to List of String. */
  implicit def mongoDbListToListOfString(dbList: MongoDBList): List[String] = {
    dbList.toList.map(_.asInstanceOf[String])
  }

  /** Appends App ID to any ID.
    * Used for distinguishing different app's data within a single collection.
    */
  def idWithAppid(appid: Int, id: String) = appid + "_" + id
}
