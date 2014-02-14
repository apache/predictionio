package io.prediction.commons.scalding.appdata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

//import io.prediction.commons.scalding.AppDataFile
import io.prediction.commons.scalding.appdata.U2iActionsSource
import io.prediction.commons.scalding.appdata.U2iActionsSource.{ FIELD_SYMBOLS }

/**
 * File Format:
 * <action>\t<uid>\t<iid>\t<t>\t<v>
 *
 * v is optional
 * use PIO_NONE if no value for optional field
 *
 * Example:
 * rate  u2  i13  123456  4
 * view  u2  i13  234567  PIO_NONE
 */
class FileU2iActionsSource(path: String, appId: Int) extends Tsv(
  p = path + "u2iActions.tsv"
) with U2iActionsSource {

  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL

  override def getSource: Source = this

  override def readData(actionField: Symbol, uidField: Symbol, iidField: Symbol, tField: Symbol, vField: Symbol)(implicit fd: FlowDef): Pipe = {
    this.read
      .mapTo((0, 1, 2, 3, 4) -> (actionField, uidField, iidField, tField, vField)) {
        fields: (String, String, String, String, String) =>
          val (action, uid, iid, t, v) = fields

          val vOpt: Option[String] = v match {
            case "PIO_NONE" => None
            case x: String => Some(x)
          }

          (action, uid, iid, t, vOpt)
      }
  }

  override def writeData(actionField: Symbol, uidField: Symbol, iidField: Symbol, tField: Symbol, vField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo((actionField, uidField, iidField, tField, vField) ->
      (FIELD_SYMBOLS("action"), FIELD_SYMBOLS("uid"), FIELD_SYMBOLS("iid"), FIELD_SYMBOLS("t"), FIELD_SYMBOLS("v"), FIELD_SYMBOLS("appid"))) {
      fields: (String, String, String, String, Option[String]) =>
        val (action, uid, iid, t, v) = fields

        val vData: String = v.getOrElse("PIO_NONE")

        (action, uid, iid, t, vData, appid)
    }.write(this)

    writtenData
  }

}