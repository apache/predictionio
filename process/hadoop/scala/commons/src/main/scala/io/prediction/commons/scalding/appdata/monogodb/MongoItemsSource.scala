package io.prediction.commons.scalding.appdata.mongodb

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef
import cascading.tuple.Tuple

import java.util.ArrayList
import java.util.HashMap

import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports._

import io.prediction.commons.scalding.MongoSource
import io.prediction.commons.scalding.appdata.ItemsSource
import io.prediction.commons.scalding.appdata.ItemsSource.FIELD_SYMBOLS

class MongoItemsSource(db: String, host: String, port: Int, appid: Int, itypes: Option[List[String]]) extends MongoSource (
    db = db,
    /*(evalId, testSet) match {
      case (Some(x), Some(false)) => "training_appdata"
      case (Some(x), Some(true)) => "test_appdata"
      case _ => "appdata"
    },*/
    coll = "items",
    cols = {
      val itemsCols = new ArrayList[String]()
      
      itemsCols.add("_id") // 0
      itemsCols.add("itypes") // 1
      itemsCols.add("appid") // 2
      
      itemsCols
    },
    mappings = {
      val itemsMappings = new HashMap[String, String]()
      
      itemsMappings.put("_id", FIELD_SYMBOLS("id").name)
      itemsMappings.put("itypes", FIELD_SYMBOLS("itypes").name)
      itemsMappings.put("appid", FIELD_SYMBOLS("appid").name)
      
      itemsMappings
    },
    query = { // read query 
      val itemsQuery = MongoDBObject("appid" -> appid) ++ (itypes.map(x => MongoDBObject("itypes" -> MongoDBObject("$in" -> x))).getOrElse(MongoDBObject()))
      
      itemsQuery
    }, 
    host = host, // String
    port = port // Int
    ) with ItemsSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource: Source = this
  
  override def readData(iidField: Symbol, itypesField: Symbol)(implicit fd: FlowDef): Pipe = {
    val items = this.read
      .mapTo((0, 1) -> (iidField, itypesField)) { fields: (String, BasicDBList) => 
        // NOTE: convert itypes form BasicDBList to scala List.
        (fields._1, fields._2.toList)
      }
    
    items
  }
  
  override def writeData(iidField: Symbol, itypesField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo((iidField, itypesField) ->
      (FIELD_SYMBOLS("id"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("appid"))) {
        fields: (String, List[String]) => 
          val (iid, itypes) = fields
          
          val itypesTuple = new Tuple()
           
          for (x <- itypes) {
            itypesTuple.add(x)
          }
          
          (iid, itypesTuple, appid)
     }.write(this)
    
     writtenData
  }
  
}

    