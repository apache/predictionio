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

  override def writeData(iidField: Symbol, simiidsField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dataPipe = p.mapTo((iidField, simiidsField) ->
      (FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("simiids"), FIELD_SYMBOLS("scores"), FIELD_SYMBOLS("simitypes"), FIELD_SYMBOLS("algoid"), FIELD_SYMBOLS("modelset"))) {
      fields: (String, List[(String, Double, List[String])]) =>
        val (iid, simiidsList) = fields

        // convert list to comma-separated string
        val simiids = simiidsList.map(_._1).mkString(",")
        val scores = simiidsList.map(_._2).mkString(",")
        val simitypes = simiidsList.map(_._3).map(x => "[" + x.mkString(",") + "]").mkString(",")

        (iid, simiids, scores, simitypes, algoid, modelSet)
    }.write(this)

    dataPipe
  }

}
