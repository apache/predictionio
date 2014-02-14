package io.prediction.commons.scalding.appdata

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

// use trait because different DB may have a bit different raw read data.
// such as time format, etc.
// The DB Source class should implement this trait so the algo implementers 
// can get the same type of pipe returned regardless of the actual DB type.

trait ItemsSource {

  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL

  def getSource: Source

  /**
   * read data and return Pipe with field name of the Symbol parameters and expected data type
   * iidField: Symbol of iid(String)
   * itypesField: Symbol of itypes(List[String])
   */
  def readData(iidField: Symbol, itypesField: Symbol)(implicit fd: FlowDef): Pipe

  /**
   * iidField: Symbol of iid(String)
   * itypesField: Symbol of itypes(List[String])
   * starttimeField: Symbol of starttime(Long)
   * endtimeField: Symbol of endtime(Option[Long])
   */
  def readStartEndtime(iidField: Symbol, itypesField: Symbol, starttimeField: Symbol, endtimeField: Symbol)(implicit fd: FlowDef): Pipe = {
    throw new RuntimeException("ItemsSource readStartEndtime is not implemented.")
  }

  /**
   * return Item Obj
   */
  def readObj(objField: Symbol)(implicit fd: FlowDef): Pipe = {
    throw new RuntimeException("ItemsSource readObj is not implemented.")
  }

  /**
   * map pipe's field data to DB table fields and write to dbSink
   * iidField: Symbol of iid(String)
   * itypesField: Symbol of itypes(List[String])
   * appid: Appid Int
   */
  def writeData(iidField: Symbol, itypesField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe

  /**
   * write Item Obj
   */
  def writeObj(objField: Symbol)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    throw new RuntimeException("ItemsSource writeObj is not implemented.")
  }
}

object ItemsSource {
  /**
   *  define the corresponding cascading Symbol name for each DB table field.
   *  ("table field name" -> Symbol)
   */
  val FIELD_SYMBOLS: Map[String, Symbol] = Map(
    ("id" -> 'id),
    ("appid" -> 'appid),
    ("ct" -> 'ct),
    ("itypes" -> 'itypes),
    ("starttime" -> 'starttime),
    ("endtime" -> 'endtime)) // optional

}