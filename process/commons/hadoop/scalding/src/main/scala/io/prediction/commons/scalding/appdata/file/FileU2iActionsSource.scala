package io.prediction.commons.scalding.appdata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

//import io.prediction.commons.scalding.AppDataFile
import io.prediction.commons.scalding.appdata.U2iActionsSource
import io.prediction.commons.scalding.appdata.U2iActionsSource.{FIELD_SYMBOLS}

class FileU2iActionsSource(path: String, appId: Int) extends Tsv (
  p = path + "u2iActions.tsv" //AppDataFile(appId, engineId, evalId, testSet, "u2iActions.tsv")
) with U2iActionsSource {
    
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource: Source = this
  
  // the file format, TAB separated file with following field for each line
  // action: String// 0
  // uid: String // 1
  // iid: String // 2
  // t: String // 3
  // v: String // 4
  // 
  // Example
  // id0<tab>3<tab>u2<tab>i13<tab>123456<tab>3
  
  override def readData(actionField: Symbol, uidField: Symbol, iidField: Symbol, tField: Symbol, vField: Symbol)(implicit fd: FlowDef): Pipe = {
    this.read
      .mapTo((0, 1, 2, 3, 4) -> (actionField, uidField, iidField, tField, vField)) { 
        fields: (String, String, String, String, String) => 
          val (action, uid, iid, t, v) = fields
          
          (action, uid, iid, t, v)
      } 
  }

  override def writeData(actionField: Symbol, uidField: Symbol, iidField: Symbol, tField: Symbol, vField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo((actionField, uidField, iidField, tField, vField) ->
      (FIELD_SYMBOLS("action"), FIELD_SYMBOLS("uid"), FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("t"), FIELD_SYMBOLS("v"), FIELD_SYMBOLS("appid"))) {
        fields: (String, String, String, String, String) =>
          val (action, uid, iid, t, v) = fields
                    
          (action.toInt, uid, iid, t, v.toInt, appid)
    }.write(this)
    
    writtenData
  }
  
}