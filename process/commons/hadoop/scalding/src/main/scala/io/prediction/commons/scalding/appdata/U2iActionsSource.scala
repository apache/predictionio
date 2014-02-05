package io.prediction.commons.scalding.appdata

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

// use trait because different DB may have a bit different raw read data.
// such as time format, etc.
// The DB Source class should implement this trait so the algo implementers 
// can get the same type of pipe returned regardless of the actual DB type.

trait U2iActionsSource {

  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL

  def getSource: Source

  /**
   * read data and return Pipe with field name of the Symbol parameters
   * actionField: Symbol of action(String)
   * uidField: Symbol of uid(String)
   * iidField: Symbol of iid(String)
   * tField: Symbol of t(String)
   * vField: Symbol of v(Option[String])
   */
  def readData(actionField: Symbol, uidField: Symbol, iidField: Symbol, tField: Symbol, vField: Symbol)(implicit fd: FlowDef): Pipe

  /**
   * map pipe's field data to DB table fields and write to dbSink.
   * actionField: Symbol of action(String)
   * uidField: Symbol of uid(String)
   * iidField: Symbol of iid(String)
   * tField: Symbol of t(String)
   * vField: Symbol of v(Option[String])
   * appid: App ID(Int)
   * p: Pipe. the data pipe.
   */
  def writeData(actionField: Symbol, uidField: Symbol, iidField: Symbol, tField: Symbol, vField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe

}

object U2iActionsSource {
  /**
   *  define the corresponding cascading Symbol name for each DB table field.
   *  ("table field name" -> Symbol)
   */
  val FIELD_SYMBOLS: Map[String, Symbol] = Map(
    ("action" -> 'action),
    ("uid" -> 'uid),
    ("iid" -> 'iid),
    ("t" -> 't),
    ("v" -> 'v),
    ("appid" -> 'appid))

}
