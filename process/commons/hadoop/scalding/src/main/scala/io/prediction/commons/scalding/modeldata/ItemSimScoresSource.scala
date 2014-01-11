package io.prediction.commons.scalding.modeldata

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

/**
 * ItemSimScoresSource
 */
trait ItemSimScoresSource {

  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL

  /**
   * return the Source object
   */
  def getSource: Source

  // TODO: readData

  /**
   * map pipe's field data to DB table fields and write to dbSink.
   * iidField: Symbol of iid(String)
   * simiidField: Symbol of simiid(String)
   * scoreField: Symbol of score(Double).
   * simitypesField: Symbol of simitypes(List[String])
   * algoid: Int. algo ID.
   * modelSet: Boolean. model set number(false means set 0, true means set 1).
   * p: Pipe. the data pipe.
   */
  def writeData(iidField: Symbol, simiidField: Symbol, scoreField: Symbol, simitypesField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe

}

object ItemSimScoresSource {

  /**
   *  define the corresponding cascading Symbol name for each DB table field.
   *  ("table field name" -> Symbol)
   */
  val FIELD_SYMBOLS: Map[String, Symbol] = Map(
    ("iid" -> 'uid),
    ("simiid" -> 'simiid),
    ("score" -> 'score),
    ("simitypes" -> 'simitypes),
    ("algoid" -> 'algoid),
    ("modelset" -> 'modelset))

}
