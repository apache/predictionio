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

class MongoItemSimScoresSource(db: String, hosts: Seq[String], ports: Seq[Int], algoid: Int, modelset: Boolean) extends MongoSource(
  db = db,
  coll = s"algo_${algoid}_${modelset}",
  cols = {
    val itemSimScoreCols = new ArrayList[String]()

    itemSimScoreCols.add("iid")
    itemSimScoreCols.add("simiids") // iid of similiar item
    itemSimScoreCols.add("scores")
    itemSimScoreCols.add("simitypes") // itypes of simiid
    itemSimScoreCols.add("algoid")
    itemSimScoreCols.add("modelset")

    itemSimScoreCols
  },
  mappings = {
    val itemSimScoreMappings = new HashMap[String, String]()

    itemSimScoreMappings.put("iid", FIELD_SYMBOLS("iid").name)
    itemSimScoreMappings.put("simiids", FIELD_SYMBOLS("simiids").name)
    itemSimScoreMappings.put("scores", FIELD_SYMBOLS("scores").name)
    itemSimScoreMappings.put("simitypes", FIELD_SYMBOLS("simitypes").name)
    itemSimScoreMappings.put("algoid", FIELD_SYMBOLS("algoid").name)
    itemSimScoreMappings.put("modelset", FIELD_SYMBOLS("modelset").name)

    itemSimScoreMappings
  },
  query = MongoDBObject(), // don't support read query
  hosts = hosts, // String
  ports = ports // Int
) with ItemSimScoresSource {

  import com.twitter.scalding.Dsl._

  override def getSource: Source = this

  override def writeData(iidField: Symbol, simiidsField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dbData = p.mapTo((iidField, simiidsField) ->
      (FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("simiids"), FIELD_SYMBOLS("scores"), FIELD_SYMBOLS("simitypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) {
      fields: (String, List[(String, Double, List[String])]) =>
        val (iid, simiidsList) = fields

        // NOTE: convert itypes List to Cascading Tuple which will become array in Mongo.
        // can't use List directly because it doesn't implement Comparable interface
        val simiidsTuple = new Tuple()
        val scoresTuple = new Tuple()
        val simitypesTuple = new Tuple()

        for (x <- simiidsList) {
          simiidsTuple.add(x._1)
          scoresTuple.add(x._2)
          val itypesOfOneItem = new Tuple()
          for (y <- x._3) {
            itypesOfOneItem.add(y)
          }
          simitypesTuple.add(itypesOfOneItem)
        }

        (iid, simiidsTuple, scoresTuple, simitypesTuple, algoid, modelSet)

    }.write(this)

    dbData
  }

}
