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
   * map pipe's field data to DB table fields and write to dbSink.
   * uidField: Symbol of uid(String).
   * iidsField: Symbol of List(iid, score, itypes). List[(String, Double, List[String])]
   * algoid: Int. algo ID. TODO: remove
   * modelset: Boolean. model set number(false means set 0, true means set 1). TODO: remove
   * p: Pipe. the data pipe.
   */
  def writeData(uidField: Symbol, iidsField: Symbol, algoid: Int, modelSet: Boolean)(p: Pipe)(implicit fd: FlowDef): Pipe

}

object ItemRecScoresSource {

  /**
   *  define the corresponding cascading Symbol name for each DB table field.
   *  ("table field name" -> Symbol)
   */
  val FIELD_SYMBOLS: Map[String, Symbol] = Map(
    ("uid" -> 'uid),
    ("iids" -> 'iids),
    ("scores" -> 'scores),
    ("itypes" -> 'itypes),
    ("algoid" -> 'algoid),
    ("modelset" -> 'modelset))

}
