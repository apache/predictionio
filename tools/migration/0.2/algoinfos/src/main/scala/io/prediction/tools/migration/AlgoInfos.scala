package io.prediction.tools.migration

import com.mongodb.casbah.Imports._
import com.typesafe.config._

object AlgoInfos {
  val config = ConfigFactory.load()

  val dbHost: String = try { config.getString("db.host") } catch { case _: Throwable => "127.0.0.1" }
  val dbPort: Int = try { config.getInt("db.port") } catch { case _: Throwable => 27017 }
  val dbName: String = try { config.getString("db.name") } catch { case _: Throwable => "predictionio" }
  val db = MongoConnection(dbHost, dbPort)(dbName)
  val algoColl = db("algos")

  def main(args: Array[String]) {
    println("PredictionIO 0.2 Migration")
    println("Fill in AlgoInfos infoid in MongoDB")
    println()
    println(s"Database host: $dbHost")
    println(s"Database port: $dbPort")
    println(s"Database name: $dbName")
    println()
    println("Looking for Algos without infoid...")
    val algos = algoColl.find(MongoDBObject("infoid" -> MongoDBObject("$exists" -> false))).toList
    if (algos.length > 0) {
      println(s"Found ${algos.length} Algos without infoid. Proceed to fill that in with default value 'pdio-knnitembased'?")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          algos map { algo =>
            algoColl.update(MongoDBObject("_id" -> algo.as[Int]("_id")), MongoDBObject("$set" -> MongoDBObject("infoid" -> "pdio-knnitembased"), "$unset" -> MongoDBObject("pkgname" -> 1)))
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
