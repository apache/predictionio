package io.prediction.tools.migration

//import com.mongodb.casbah.Imports._
import com.typesafe.config._
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.Imports.{MongoConnection}

object Settings04 {
  val config = ConfigFactory.load()

  val dbHost: String = try { config.getString("db.host") } catch { case _: Throwable => "127.0.0.1" }
  val dbPort: Int = try { config.getInt("db.port") } catch { case _: Throwable => 27017 }
  val dbName: String = try { config.getString("db.name") } catch { case _: Throwable => "predictionio" }
  val db = MongoConnection(dbHost, dbPort)(dbName)
  val engineColl = db("engines")
  val offlineEvalColl = db("offlineEvals")
  val offlineEvalSplitterColl = db("offlineEvalSplitters")
  val offlineEvalResultsColl = db("offlineEvalResults")
  val algoColl = db("algos")

  val seqColl = db("seq")

  /** Get the next sequence number from the given sequence name. */
  def genNext(name: String): Int = {
    val qFind = MongoDBObject("_id" -> name)
    val qField = MongoDBObject("next" -> 1)
    val qSort = MongoDBObject()
    val qRemove = false
    val qModify = $inc("next" -> 1)
    val qReturnNew = true
    val qUpsert = true
    seqColl.findAndModify(qFind, qField, qSort, qRemove, qModify, qReturnNew, qUpsert).get.getAsOrElse[Number]("next", 0).intValue
  }

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

    //
    println()
    println("Looking for OfflineEvals without OfflineEvalSplitter...")
    // create OfflineEvalSplitter for existing offlineEvalRecords which don't have one
    val offlineEvals = offlineEvalColl.find()
    val offlineEvalWithoutSplitter = (offlineEvals filter { eval => (offlineEvalSplitterColl.find(MongoDBObject("evalid" -> eval.as[Int]("_id"))).count == 0) }).toStream
    val offlineEvalWithoutSplitterSize = offlineEvalWithoutSplitter.size
    if (offlineEvalWithoutSplitterSize > 0) {
      println(s"Found ${offlineEvalWithoutSplitterSize} OfflineEvals without OfflineEvalSplitter. Proceed to add OfflineEvalSplitter for these records?")   
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          offlineEvalWithoutSplitter foreach { eval =>
            val id = genNext("offlineEvalSplitterId")
            val evalid = eval.as[Int]("_id")
            println(s"Insert OfflineEvalSplitter for OfflineEval ID = $evalid")
            offlineEvalSplitterColl.insert(MongoDBObject(
              "_id" -> id,
              "evalid" -> evalid,
              "name" -> ("sim-eval-" + evalid + "-splitter"),
              "infoid" -> "trainingtestsplit",
              "settings" -> Map(
                "trainingPercent" -> 0.8,
                "validationPercent" -> 0.0,
                "testPercent" -> 0.2,
                "timeorder" -> false
                )
            ))
          }
          
          println("Done")
        }
        case _ => println("Aborted")
      }
    } else {
      println("None found")
    }

    //
    println()
    println("Looking for OfflineEvals with obsolete fields trainingsize, testsize, timeorder...")
    val oldFieldExists = $or(("trainingsize" -> MongoDBObject("$exists" -> true)),
      ("testsize" -> MongoDBObject("$exists" -> true)),
      ("timeorder" -> MongoDBObject("$exists" -> true)))

    val offlineEvalsWithOldFields = offlineEvalColl.find(oldFieldExists)
    if (offlineEvalsWithOldFields.length > 0) {
      println(s"Found ${offlineEvalsWithOldFields.length} OfflineEvals with obsolete fields. Proceed to remove these obsolete fields from the records?")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          offlineEvalsWithOldFields foreach { eval =>
            val evalid = eval.as[Int]("_id")
            println(s"Remove obsolete fields for OfflineEval ID = $evalid")
            offlineEvalColl.update(MongoDBObject("_id" -> evalid), MongoDBObject("$unset" -> MongoDBObject("trainingsize" -> 1, "testsize" -> 1, "timeorder" -> 1)) )
          }
          println("Done")
        }
        case _ => println("Aborted")
      }
    } else {
      println("None found")
    }

    //
    println()
    println("Looking for OfflineEvals without iterations...")
    val offlineEvalsWithoutIterations = offlineEvalColl.find(MongoDBObject("iterations" -> MongoDBObject("$exists" -> false)))

    if (offlineEvalsWithoutIterations.length > 0) {
      println(s"Found ${offlineEvalsWithoutIterations.length} OfflineEvals without iterations. Proceed to add the 'iterations' field to these records?")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          offlineEvalsWithoutIterations foreach { eval =>
            val evalid = eval.as[Int]("_id")
            println(s"Update OfflineEval ID = $evalid with 'iterations'")
            offlineEvalColl.update(MongoDBObject("_id" -> evalid), MongoDBObject("$set" -> MongoDBObject("iterations" -> 1)) )
          }
          println("Done")
        }
        case _ => println("Aborted")
      }
    } else {
      println("None Found")
    }

    //
    println()
    println("Looking for OfflineEvalResults without iteration or splitset...")
    val offlineEvalResultsAll = offlineEvalResultsColl.find()

    val offlineEvalResultsSelected = (offlineEvalResultsAll filter { result =>
      val iteration: Option[Int] = result.getAs[Int]("iteration")
      val splitset: Option[String] = result.getAs[String]("splitset")

      ((iteration == None) || ((splitset != Some("test")) && (splitset != Some("validation"))) )}).toStream

    val offlineEvalResultsSelectedSize = offlineEvalResultsSelected.size
    if (offlineEvalResultsSelectedSize > 0) {
      println(s"Found ${offlineEvalResultsSelectedSize} OfflineEvalResults without proper iteration or splitset field. Proceed to update?")

      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          offlineEvalResultsSelected foreach { result =>

            val resultid = result.as[String]("_id")
            val evalid = result.as[Int]("evalid")
            val metricid = result.as[Int]("metricid")
            val algoid = result.as[Int]("algoid")
            val score = result.as[Double]("score")
            val iteration: Option[Int] = result.getAs[Int]("iteration")
            val splitset: Option[String] = result.getAs[String]("splitset")

            val newIteration: Int = iteration match {
              case None => 1
              case Some(x) => x
            }
            val newSplitset: String = splitset match {
              case Some("test") => "test"
              case Some("validation") => "validation"
              case _ => "test"
            }
                      
            val newResultid = (evalid + "_" + metricid + "_" + algoid + "_" + newIteration + "_" + newSplitset)
            println(s"Update OfflineEvalResult ID = $resultid. New ID = $newResultid")
            
            offlineEvalResultsColl.save(MongoDBObject(
              "_id" -> newResultid,
              "evalid" -> evalid,
              "metricid" -> metricid,
              "algoid" -> algoid,
              "score" -> score,
              "iteration" -> newIteration,
              "splitset" -> newSplitset
            ))
            offlineEvalResultsColl.remove(MongoDBObject("_id" -> resultid))
              
          }
          println("Done")
        }
        case _ => println("Aborted")
      }

    } else {
      println("None Found")
    }

    //
    println()
    println("Looking for Algo without status...")
    val algoWithoutStatus = algoColl.find(MongoDBObject("status" -> MongoDBObject("$exists" -> false)))
    if (algoWithoutStatus.length > 0) {
      println(s"Found ${algoWithoutStatus.length} Algos without status. Proceed to add the 'status' field to these records?")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          algoWithoutStatus foreach { algo =>
            val offlineevalid = algo.getAs[Int]("offlineevalid")
            val deployed = algo.as[Boolean]("deployed")
            val status: String = (offlineevalid, deployed) match {
              case (None, true) => "deployed"
              case (None, false) => "ready"
              case (Some(x), _) => "simeval"
            }
            val algoid = algo.as[Int]("_id")
            println(s"Update Algo ID = $algoid with 'status'")
            algoColl.update(MongoDBObject("_id" -> algoid), 
              MongoDBObject("$set" -> MongoDBObject("status" -> status)))
          }
          println("Done")
        }
        case _ => println("Aborted")
      } 
    } else {
      println("None Found")
    }

  }

}
