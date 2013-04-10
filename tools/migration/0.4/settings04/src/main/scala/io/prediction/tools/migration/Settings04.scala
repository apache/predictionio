package io.prediction.tools.migration

import com.mongodb.casbah.Imports._
import com.typesafe.config._

object Settings04 {
  val config = ConfigFactory.load()

  val dbHost: String = try { config.getString("db.host") } catch { case _: Throwable => "127.0.0.1" }
  val dbPort: Int = try { config.getInt("db.port") } catch { case _: Throwable => 27017 }
  val dbName: String = try { config.getString("db.name") } catch { case _: Throwable => "predictionio" }
  val db = MongoConnection(dbHost, dbPort)(dbName)
  val engineColl = db("engines")

  def main(args: Array[String]) {
    println("PredictionIO 0.4 Migration")
    println("Convert Engine.enginetype to Engine.infoid in MongoDB")
    println()
    println(s"Database host: $dbHost")
    println(s"Database port: $dbPort")
    println(s"Database name: $dbName")
    println()
    println("Looking for Engines without infoid...")
    val engines = engineColl.find(MongoDBObject("infoid" -> MongoDBObject("$exists" -> false))).toList
    if (engines.length > 0) {
      println(s"Found ${engines.length} Engines without infoid. Proceed to convert enginetype to infoid?")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          engines map { engine =>
            engineColl.update(MongoDBObject("_id" -> engine.as[Int]("_id")), MongoDBObject("$set" -> MongoDBObject("infoid" -> engine.as[String]("enginetype")), "$unset" -> MongoDBObject("enginetype" -> 1)))
          }
          println("Done")
        }
        case _ => println("Aborted")
      }
    } else {
      println("None found")
    }
  }
}
