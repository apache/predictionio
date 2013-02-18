package io.prediction.commons.scalding.appdata

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

// use trait because different DB may have a bit different raw read data.
// such as time format, etc.
// The DB Source class should implement this trait so the algo implementers 
// can get the same type of pipe returned regardless of the actual DB type.

trait UsersSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  def getSource: Source
  
  /**
   * read data and return Pipe with field name of the Symbol parameters and expected data type
   * uidField: Symbol of iid(String)
   */
  def readData(uidField: Symbol)(implicit fd: FlowDef): Pipe
  
  /**
   * map pipe's field data to DB table fields and write to dbSink
   * uidField: Symbol of iid(String)
   * appid: Appid Int
   */
  def writeData(uidField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe
  
}

object UsersSource {
  /**
   *  define the corresponding cascading Symbol name for each DB table field.
   *  ("table field name" -> Symbol)
   */
  val FIELD_SYMBOLS: Map[String, Symbol] = Map(
      ("id" -> 'id),
      ("appid" -> 'appid))
      
}