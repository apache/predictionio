package io.prediction.commons.scalding.appdata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

import io.prediction.commons.scalding.appdata.UsersSource
import io.prediction.commons.scalding.appdata.UsersSource.FIELD_SYMBOLS

class FileUsersSource(path: String, appId: Int) extends Tsv (
  p = path + "users.tsv"
) with UsersSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource: Source = this
  
  // file format, TAB separated file with following field for each line
  
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