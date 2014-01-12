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

  override def writeData(uidField: Symbol, iidsField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dataPipe = p.mapTo((uidField, iidsField) ->
      (FIELD_SYMBOLS("uid"), FIELD_SYMBOLS("iids"), FIELD_SYMBOLS("scores"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) {
      fields: (String, List[(String, Double, List[String])]) =>
        val (uid, iidsList) = fields

        // convert list to comma-separated String
        val iids = iidsList.map(_._1).mkString(",")
        val scores = iidsList.map(_._2).mkString(",")
        val itypes = iidsList.map(_._3).map(x => "[" + x.mkString(",") + "]").mkString(",")

        (uid, iids, scores, itypes, algoid, modelSet)

    }.write(this)

    dataPipe
  }

}