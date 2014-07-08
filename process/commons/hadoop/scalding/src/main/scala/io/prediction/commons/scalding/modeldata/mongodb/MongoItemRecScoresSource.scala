package io.prediction.commons.scalding.modeldata.mongodb

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef
import cascading.tuple.Tuple

import java.util.ArrayList
import java.util.HashMap

import com.mongodb.casbah.Imports._

import io.prediction.commons.scalding.MongoSource
import io.prediction.commons.scalding.modeldata.ItemRecScoresSource
import io.prediction.commons.scalding.modeldata.ItemRecScoresSource.FIELD_SYMBOLS

class MongoItemRecScoresSource(db: String, hosts: Seq[String], ports: Seq[Int], algoid: Int, modelset: Boolean) extends MongoSource(
  db = db,
  coll = s"algo_${algoid}_${modelset}",
  cols = {
    val itemRecScoreCols = new ArrayList[String]()
    itemRecScoreCols.add("uid")
    itemRecScoreCols.add("iids")
    itemRecScoreCols.add("scores")
    itemRecScoreCols.add("itypes")
    itemRecScoreCols.add("algoid")
    itemRecScoreCols.add("modelset")

    itemRecScoreCols
  },
  mappings = {
    val itemRecScoreMappings = new HashMap[String, String]()
    itemRecScoreMappings.put("uid", FIELD_SYMBOLS("uid").name)
    itemRecScoreMappings.put("iids", FIELD_SYMBOLS("iids").name)
    itemRecScoreMappings.put("scores", FIELD_SYMBOLS("scores").name)
    itemRecScoreMappings.put("itypes", FIELD_SYMBOLS("itypes").name)
    itemRecScoreMappings.put("algoid", FIELD_SYMBOLS("algoid").name)
    itemRecScoreMappings.put("modelset", FIELD_SYMBOLS("modelset").name)

    itemRecScoreMappings
  },
  query = MongoDBObject(), // don't support read query
  hosts = hosts, // String
  ports = ports // Int
) with ItemRecScoresSource {

  import com.twitter.scalding.Dsl._

  override def getSource: Source = this

  override def writeData(uidField: Symbol, iidsField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dbData = p.mapTo((uidField, iidsField) ->
      (FIELD_SYMBOLS("uid"), FIELD_SYMBOLS("iids"), FIELD_SYMBOLS("scores"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) {
      fields: (String, List[(String, Double, List[String])]) =>
        val (uid, iidsList) = fields

        // NOTE: convert itypes List to Cascading Tuple which will become array in Mongo.
        // can't use List directly because it doesn't implement Comparable interface
        val iidsTuple = new Tuple()
        val scoresTuple = new Tuple()
        val itypesTuple = new Tuple()

        for (x <- iidsList) {
          iidsTuple.add(x._1)
          scoresTuple.add(x._2)
          val itypesOfOneItem = new Tuple()
          for (y <- x._3) {
            itypesOfOneItem.add(y)
          }
          itypesTuple.add(itypesOfOneItem)
        }

        (uid, iidsTuple, scoresTuple, itypesTuple, algoid, modelSet)

    }.write(this)

    dbData
  }

}
