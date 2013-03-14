package io.prediction.commons.scalding.modeldata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

//import io.prediction.commons.scalding.ModelDataFile
import io.prediction.commons.scalding.modeldata.ItemSimScoresSource
import io.prediction.commons.scalding.modeldata.ItemSimScoresSource.FIELD_SYMBOLS

class FileItemSimScoresSource(path: String) extends Tsv(
    p = path + "itemSimScores.tsv" //ModelDataFile(appId, engineId, algoId, evalId, "itemSimScores.tsv")
    ) with ItemSimScoresSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource = this
  
  override def writeData(iidField: Symbol, simiidField: Symbol, scoreField: Symbol, simitypesField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dataPipe = p.mapTo((iidField, simiidField, scoreField, simitypesField) ->
       (FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("simiid"), FIELD_SYMBOLS("score"), FIELD_SYMBOLS("simitypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) {
         fields: (String, String, Double, List[String]) =>
          val (iid, simiid, score, simitypes) = fields
          
          (iid, simiid, score, simitypes.mkString(","), algoid, modelSet)
    }.write(this)

    dataPipe
  }
  
}
