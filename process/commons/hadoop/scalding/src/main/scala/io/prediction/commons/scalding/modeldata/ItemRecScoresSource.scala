package io.prediction.commons.scalding.modeldata

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

/**
 * ItemRecScoresSource
 */
trait ItemRecScoresSource {

  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL

  /**
   * return the Source object
   */
  def getSource: Source

  /**
   * read data and return Pipe with field name of the Symbol parameters and expected data type
   * uidField: Symbol of iid(String)
   * iidField: Symbol of iid(String).
   * scoreField: Symbol of score(Double).
   * itypesField: Symbol of itypes(List[String])
   */
  def readData(uidField: Symbol, iidField: Symbol, scoreField: Symbol, itypesField: Symbol)(implicit fd: FlowDef): Pipe

  /**
   * map pipe's field data to DB table fields and write to dbSink.
   * uidField: Symbol of uid(String).
   * iidField: Symbol of iid(String).
   * scoreField: Symbol of score(Double).
   * itypesField: Symbol of itypes(List[String]).
   * algoid: Int. algo ID.
   * modelset: Boolean. model set number(false means set 0, true means set 1).
   * p: Pipe. the data pipe.
   */
  def writeData(uidField: Symbol, iidField: Symbol, scoreField: Symbol, itypesField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe

}

object ItemRecScoresSource {

  /**
   *  define the corresponding cascading Symbol name for each DB table field.
   *  ("table field name" -> Symbol)
   */
  val FIELD_SYMBOLS: Map[String, Symbol] = Map(
    ("uid" -> 'uid),
    ("iid" -> 'iid),
    ("score" -> 'score),
    ("itypes" -> 'itypes),
    ("algoid" -> 'algoid),
    ("modelset" -> 'modelset))

}
