package io.prediction.commons.scalding.modeldata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

//import io.prediction.commons.scalding.ModelDataFile
import io.prediction.commons.scalding.modeldata.ItemRecScoresSource
import io.prediction.commons.scalding.modeldata.ItemRecScoresSource.FIELD_SYMBOLS

class FileItemRecScoresSource(path: String) extends Tsv(
    p = path + "itemRecScores.tsv" //ModelDataFile(appId, engineId, algoId, evalId, "itemRecScores.tsv")
    ) with ItemRecScoresSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource = this
  
  override def readData(uidField: Symbol, iidField: Symbol, scoreField: Symbol, itypesField: Symbol)(implicit fd: FlowDef): Pipe = {
    this.read
      .mapTo((0, 1, 2, 3) -> (uidField, iidField, scoreField, itypesField)) { fields: (String, String, Double, String) =>
        val (uid, iid, score, itypes) = fields
        
        // NOTE: itypes are comma separated String in file, convert it to List[String]
        (uid, iid, score, itypes.split(",").toList)
    }
  }
  
  override def writeData(uidField: Symbol, iidField: Symbol, scoreField: Symbol, itypesField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dataPipe = p.mapTo((uidField, iidField, scoreField, itypesField) -> 
    (FIELD_SYMBOLS("uid"), FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("score"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) { 
      fields: (String, String, Double, List[String]) =>
        val (uid, iid, score, itypes) = fields
        
        // NOTE: itypes are List[String], convert it to comma separated String.
        (uid, iid, score, itypes.mkString(","), algoid, modelSet)
      
    }.write(this)
    
    dataPipe
  }
  
}