package io.prediction.commons.scalding.appdata.mongodb

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef
import cascading.tuple.Tuple

import java.util.ArrayList
import java.util.HashMap

import org.scala_tools.time.Imports._

import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers

import io.prediction.commons.scalding.MongoSource
import io.prediction.commons.scalding.appdata.UsersSource
import io.prediction.commons.scalding.appdata.UsersSource.FIELD_SYMBOLS
import io.prediction.commons.appdata.{User}

class MongoUsersSource(db: String, host: String, port: Int, appid: Int) extends MongoSource (
    db = db,
    coll = "users",
    cols = {
      val usersCols = new ArrayList[String]()
      
      usersCols.add("_id") // 0
      usersCols.add("appid") // 1
      usersCols.add("ct") // 2
      
      usersCols
    },
    mappings = {
      val usersMappings = new HashMap[String, String]()
      
      usersMappings.put("_id", FIELD_SYMBOLS("id").name)
      usersMappings.put("appid", FIELD_SYMBOLS("appid").name)
      usersMappings.put("ct", FIELD_SYMBOLS("ct").name)
      
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
  
  RegisterJodaTimeConversionHelpers()

  override def getSource: Source = this
  
  override def readData(uidField: Symbol)(implicit fd: FlowDef): Pipe = {
    val users = this.read
      .mapTo((0) -> (uidField)) { fields: String => 

        fields
      }
    
    users
  }
  
  override def readObj(objField: Symbol)(implicit fd: FlowDef): Pipe = {
    val users = this.read
      .mapTo((0, 1, 2) -> objField) { fields: (String, Int, DateTime) =>
        val (id, appid, ct) = fields

        User(
          id = id,
          appid = appid,
          ct = ct,
          latlng = None,
          inactive = None,
          attributes = None
        )
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

  override def writeObj(objField: Symbol)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo((objField) ->
      (FIELD_SYMBOLS("id"), FIELD_SYMBOLS("appid"), FIELD_SYMBOLS("ct"))) { obj: User =>

      (obj.id, obj.appid, obj.ct)
    }.write(this)

    writtenData
  }
  
}

    