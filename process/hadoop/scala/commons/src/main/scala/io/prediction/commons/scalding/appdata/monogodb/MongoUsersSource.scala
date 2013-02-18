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
import io.prediction.commons.scalding.appdata.UsersSource
import io.prediction.commons.scalding.appdata.UsersSource.FIELD_SYMBOLS

class MongoUsersSource(db: String, host: String, port: Int, appid: Int) extends MongoSource (
    db = db,
    coll = "users",
    cols = {
      val usersCols = new ArrayList[String]()
      
      usersCols.add("_id") // 0
      usersCols.add("appid") // 1
      
      usersCols
    },
    mappings = {
      val usersMappings = new HashMap[String, String]()
      
      usersMappings.put("_id", FIELD_SYMBOLS("id").name)
      usersMappings.put("appid", FIELD_SYMBOLS("appid").name)
      
      usersMappings
    },
    query = { // read query 
      val usersQuery = MongoDBObject("appid" -> appid)

      usersQuery
    }, 
    host = host, // String
    port = port // Int
    ) with UsersSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource: Source = this
  
  override def readData(uidField: Symbol)(implicit fd: FlowDef): Pipe = {
    val users = this.read
      .mapTo((0) -> (uidField)) { fields: String => 

        fields
      }
    
    users
  }
  
  override def writeData(uidField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo((uidField) ->
      (FIELD_SYMBOLS("id"), FIELD_SYMBOLS("appid"))) {
        fields: String => 
          val uid = fields

          (uid, appid)
     }.write(this)
    
     writtenData
  }
  
}

    