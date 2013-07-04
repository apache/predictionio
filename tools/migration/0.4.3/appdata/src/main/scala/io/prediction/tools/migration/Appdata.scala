package io.prediction.tools.migration

//import com.typesafe.config._
import io.prediction.commons.{Config}
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.Imports.{MongoConnection}
import com.mongodb.WriteResult

object Appdata {
  //val config = ConfigFactory.load()
  val config = new Config()

  val appdataDbName = config.appdataDbName
  val appdataTrainingDbName = config.appdataTrainingDbName
  val appdataValidationDbName = config.appdataValidationDbName
  val appdataTestDbName = config.appdataTestDbName

  val appdataDb = config.appdataMongoDb.get
  val trainingDb = config.appdataTrainingMongoDb.get
  val validationDb = config.appdataValidationMongoDb.get
  val testDb = config.appdataTestMongoDb.get

  val dbSeq = Seq(
    appdataDbName -> appdataDb, 
    appdataTrainingDbName -> trainingDb,
    appdataValidationDbName -> validationDb,
    appdataTestDbName -> testDb )

  def main(args: Array[String]) {
    println("PredictionIO 0.4 to 0.4.3 Migration")
    println()
    println()

    val emptyObj = MongoDBObject()

    for ( (name, db) <- dbSeq ) {
     
      println(s"\nChecking ${name} DB...")

      val usersColl = db("users")
      val itemsColl = db("items")
      val u2iActionsColl = db("u2iActions")

      // users
      print("Updating user records with custom attributes...")

      val usersWithAttributes = usersColl.find(MongoDBObject("attributes" -> MongoDBObject("$exists" -> true)))
      var userCount = 0
      while (usersWithAttributes.hasNext) {
        val dbObj = usersWithAttributes.next
        userCount += 1
        val attributes = dbObj.getAs[DBObject]("attributes") map { dbObjToMap(_) }
        val flattenedObj = attributes map { a => attributesToMongoDBObject(a) } getOrElse emptyObj
       
        usersColl.update(MongoDBObject("_id" -> dbObj.as[String]("_id")), MongoDBObject("$set" -> flattenedObj, "$unset" -> MongoDBObject("attributes" -> 1)))
      }
      println(s"${userCount} users were updated.")

      // items
      print("Updating item records with custom attributes...")
      val itemsWithAttributes = itemsColl.find(MongoDBObject("attributes" -> MongoDBObject("$exists" -> true)))

      var itemCount = 0
      while (itemsWithAttributes.hasNext) {
        val dbObj = itemsWithAttributes.next
        itemCount += 1
        val attributes = dbObj.getAs[DBObject]("attributes") map { dbObjToMap(_) }
        val flattenedObj = attributes map { a => attributesToMongoDBObject(a) } getOrElse emptyObj
       
        itemsColl.update(MongoDBObject("_id" -> dbObj.as[String]("_id")), MongoDBObject("$set" -> flattenedObj, "$unset" -> MongoDBObject("attributes" -> 1)))
      }
      println(s"${itemCount} items were updated.")
   
      // u2iActions
      val rate: Int = 0
      val likeDislike: Int = 1 //(v==1 => like, v==0 => dislike)
      val view: Int = 2
      val viewDetails: Int = 3
      val conversion: Int = 4

      def checkWriteResult(wr: WriteResult) = {
        if (wr.getLastError().ok()) {
          println(s"${wr.getN} records were updated.")
        } else {
          wr.getLastError().throwOnError()
        }
      }

      print("Updating rate actions to new action name (may take a while)...")
      val rateResult = u2iActionsColl.update(MongoDBObject("action" -> rate), MongoDBObject("$set" -> MongoDBObject("action" -> "rate")), false, true)
      checkWriteResult(rateResult)

      print("Updating like actions to new action name (may take a while)...") 
      val likeResult = u2iActionsColl.update(MongoDBObject("action" -> likeDislike, "v" -> 1), MongoDBObject("$set" -> MongoDBObject("action" -> "like"), "$unset" -> MongoDBObject("v" -> 1)), false, true)
      checkWriteResult(likeResult)

      print("Updating dislike actions to new action name (may take a while)...")
      val dislikeResult = u2iActionsColl.update(MongoDBObject("action" -> likeDislike, "v" -> 0), MongoDBObject("$set" -> MongoDBObject("action" -> "dislike"), "$unset" -> MongoDBObject("v" -> 1)), false, true)
      checkWriteResult(dislikeResult)

      print("Updating view actions to new action name (may take a while)...")
      val viewResult = u2iActionsColl.update(MongoDBObject("action" -> view), MongoDBObject("$set" -> MongoDBObject("action" -> "view")), false, true)
      checkWriteResult(viewResult)

      print("Updating viewDetails actions to new action name (may take a while)...")
      val viewDetailsResult = u2iActionsColl.update(MongoDBObject("action" -> viewDetails), MongoDBObject("$set" -> MongoDBObject("action" -> "viewDetails")), false, true)
      checkWriteResult(viewDetailsResult)

      print("Updating conversion actions to new action name (may take a while)...")
      val conversionResult = u2iActionsColl.update(MongoDBObject("action" -> conversion), MongoDBObject("$set" -> MongoDBObject("action" -> "conversion")), false, true)
      checkWriteResult(conversionResult)
      
    }

    println("Done.")

  }

  /** Modified from Salat. */
  def dbObjToMap(dbObj: DBObject): Map[String, Any] = {
    val builder = Map.newBuilder[String, Any]

    dbObj foreach {
      case (field, value) =>
        builder += field -> value
    }

    builder.result()
  }

  def attributesToMongoDBObject(attributes: Map[String, Any]) = {
    MongoDBObject( (attributes map { case (k, v) => ("ca_" + k, v) }).toList )
  }

}