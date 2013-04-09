package io.prediction.commons.scalding.appdata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

import org.joda.time.DateTime

import io.prediction.commons.scalding.appdata.UsersSource
import io.prediction.commons.scalding.appdata.UsersSource.FIELD_SYMBOLS
import io.prediction.commons.appdata.{User}

/**
 * File Format:
 * <id>\t<appid>\t<ct>
 *
 * Example:
 * 1  2  12345
 */
class FileUsersSource(path: String, appId: Int) extends Tsv (
  p = path + "users.tsv"
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
  
  override def readObj(objField: Symbol)(implicit fd: FlowDef): Pipe = {
    val users = this.read
      .mapTo((0, 1, 2) -> objField) { fields: (String, Int, Long) =>
        val (id, appid, ct) = fields

        User(
          id = id,
          appid = appid,
          ct = new DateTime(ct),
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

      val ct: java.util.Date = obj.ct.toDate()

      (obj.id, obj.appid, ct.getTime())
    }.write(this)

    writtenData
  }
  
}