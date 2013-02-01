package io.prediction.commons

import com.mongodb.casbah.Imports._

object MongoUtils {
  /** Modified from Salat. */
  def dbObjToMap(dbObj: DBObject): Map[String, Any] = {
    val builder = Map.newBuilder[String, Any]

    dbObj foreach {
      case (field, value) =>
        builder += field -> value
    }

    builder.result()
  }
}
