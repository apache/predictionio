package io.prediction.commons.scalding.settings.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

//import io.prediction.commons.scalding.OfflineEvalDataFile
import io.prediction.commons.scalding.settings.OfflineEvalResultsSource

/**
 * File Format:
 * <eval id>\t<metric id>\t<algo id>\t<score>\t<iteration>
 *
 * Example:
 * 8  4  5  0.123456  3
 */
class FileOfflineEvalResultsSource(path: String) extends Tsv(
    p = path + "/offlineEvalResults.tsv" //OfflineEvalDataFile(appId, engineId, evalId, metricId, algoId, name="offlineEvalResults.tsv")
) with OfflineEvalResultsSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource: Source = this
  
  override def writeData(evalidField: Symbol, metricidField: Symbol, algoidField: Symbol, scoreField: Symbol, iterationField: Symbol)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dataPipe = p.project(evalidField, metricidField, algoidField, scoreField, iterationField)
                    .write(this)

    dataPipe
  }
}