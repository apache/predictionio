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

class MongoItemRecScoresSource(db: String, host: String, port: Int) extends MongoSource (
    db = db,
    coll = "itemRecScores",
    cols = {
      val itemRecScoreCols = new ArrayList[String]()
      itemRecScoreCols.add("uid")
      itemRecScoreCols.add("iid")
      itemRecScoreCols.add("score")
      itemRecScoreCols.add("itypes")
      itemRecScoreCols.add("algoid")
      itemRecScoreCols.add("modelset")

      itemRecScoreCols
    },
    mappings = {
      val itemRecScoreMappings = new HashMap[String, String]()
      itemRecScoreMappings.put("uid", FIELD_SYMBOLS("uid").name)
      itemRecScoreMappings.put("iid", FIELD_SYMBOLS("iid").name)
      itemRecScoreMappings.put("score", FIELD_SYMBOLS("score").name)
      itemRecScoreMappings.put("itypes", FIELD_SYMBOLS("itypes").name)
      itemRecScoreMappings.put("algoid", FIELD_SYMBOLS("algoid").name)
      itemRecScoreMappings.put("modelset", FIELD_SYMBOLS("modelset").name)

      itemRecScoreMappings
    },
    query = MongoDBObject(), // don't support read query
    host = host, // String
    port = port // Int
    ) with ItemRecScoresSource {

  import com.twitter.scalding.Dsl._

  override def getSource: Source = this


  override def readData(uidField: Symbol, iidField: Symbol, scoreField: Symbol, itypesField: Symbol)(implicit fd: FlowDef): Pipe = {
    val itemRecScores = this.read
      .mapTo((0, 1, 2, 3) -> (uidField, iidField, scoreField, itypesField)) {
        fields: (String, String, Double, BasicDBList) =>
          val (uid, iid, score, itypes) = fields

          (uid, iid, score, itypes.toList)
      }
    itemRecScores
  }

  override def writeData(uidField: Symbol, iidField: Symbol, scoreField: Symbol, itypesField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dbData = p.mapTo((uidField, iidField, scoreField, itypesField) ->
    (FIELD_SYMBOLS("uid"), FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("score"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) {
      fields: (String, String, Double, List[String]) =>
        val (uid, iid, score, itypes) = fields

        // NOTE: convert itypes List to Cascading Tuple which will become array in Mongo.
        // can't use List directly because it doesn't implement Comparable interface
        val itypesTuple = new Tuple()

        for (x <- itypes) {
          itypesTuple.add(x)
        }

        (uid, iid, score, itypesTuple, algoid, modelSet)

    }.write(this)

    dbData
  }


}
