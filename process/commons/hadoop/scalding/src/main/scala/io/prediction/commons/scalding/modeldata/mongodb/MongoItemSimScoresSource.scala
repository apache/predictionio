package io.prediction.commons.scalding.modeldata.mongodb

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef
import cascading.tuple.Tuple

import java.util.ArrayList
import java.util.HashMap

import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports.MongoDBObject

import io.prediction.commons.scalding.MongoSource
import io.prediction.commons.scalding.modeldata.ItemSimScoresSource
import io.prediction.commons.scalding.modeldata.ItemSimScoresSource.FIELD_SYMBOLS

class MongoItemSimScoresSource(db: String, host: String, port: Int) extends MongoSource(
  db = db,
  coll = "itemSimScores",
  cols = {
    val itemSimScoreCols = new ArrayList[String]()

    itemSimScoreCols.add("iid")
    itemSimScoreCols.add("simiid") // iid of similiar item
    itemSimScoreCols.add("score")
    itemSimScoreCols.add("simitypes") // itypes of simiid
    itemSimScoreCols.add("algoid")
    itemSimScoreCols.add("modelset")

    itemSimScoreCols
  },
  mappings = {
    val itemSimScoreMappings = new HashMap[String, String]()

    itemSimScoreMappings.put("iid", FIELD_SYMBOLS("iid").name)
    itemSimScoreMappings.put("simiid", FIELD_SYMBOLS("simiid").name)
    itemSimScoreMappings.put("score", FIELD_SYMBOLS("score").name)
    itemSimScoreMappings.put("simitypes", FIELD_SYMBOLS("simitypes").name)
    itemSimScoreMappings.put("algoid", FIELD_SYMBOLS("algoid").name)
    itemSimScoreMappings.put("modelset", FIELD_SYMBOLS("modelset").name)

    itemSimScoreMappings
  },
  query = MongoDBObject(), // don't support read query
  host = host, // String 
  port = port // Int
) with ItemSimScoresSource {

  import com.twitter.scalding.Dsl._

  override def getSource: Source = this

  override def writeData(iidField: Symbol, simiidField: Symbol, scoreField: Symbol, simitypesField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dbData = p.mapTo((iidField, simiidField, scoreField, simitypesField) ->
      (FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("simiid"), FIELD_SYMBOLS("score"), FIELD_SYMBOLS("simitypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) {
      fields: (String, String, Double, List[String]) =>
        val (iid, simiid, score, simitypes) = fields

        // NOTE: convert itypes List to Cascading Tuple which will become array in Mongo.
        // can't use List directly because it doesn't implement Comparable interface
        val simitypesTuple = new Tuple()

        for (x <- simitypes) {
          simitypesTuple.add(x)
        }

        (iid, simiid, score, simitypesTuple, algoid, modelSet)

    }.write(this)

    dbData
  }

}