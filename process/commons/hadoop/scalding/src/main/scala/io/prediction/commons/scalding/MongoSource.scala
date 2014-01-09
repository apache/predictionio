package io.prediction.commons.scalding

import com.twitter.scalding._

import cascading.tap.Tap

import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.OutputCollector
import org.apache.hadoop.mapred.RecordReader

import com.clojurewerkz.cascading.mongodb.MongoDBScheme
import com.clojurewerkz.cascading.mongodb.MongoDBTap
import com.mongodb.casbah.Imports.MongoDBObject
import com.mongodb.DBObject

import java.util.List
import java.util.ArrayList
import java.util.Map
import java.util.HashMap

case class MongoSource(db: String, coll: String, cols: List[String], mappings: Map[String, String], query: DBObject, host: String = "127.0.0.1", port: Int = 27017) extends Source {

  val mongoScheme = new MongoDBScheme(host, port, db, coll, cols, mappings, query)

  // auxiliary constructor for no-query case
  def this(db: String, coll: String, cols: List[String], mappings: Map[String, String], host: String = "127.0.0.1", port: Int = 27017) =
    this(db, coll, cols, mappings, MongoDBObject(), host, port)

  protected def castMongoTap(tap: MongoDBTap): Tap[JobConf, RecordReader[_, _], OutputCollector[_, _]] = {
    tap.asInstanceOf[Tap[JobConf, RecordReader[_, _], OutputCollector[_, _]]]

  }

  override def createTap(readOrWrite: AccessMode)(implicit mode: Mode): Tap[_, _, _] = {
    castMongoTap(new MongoDBTap(mongoScheme))
  }
}
