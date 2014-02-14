package io.prediction.commons.scalding.appdata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

import org.joda.time.DateTime

//import io.prediction.commons.scalding.AppDataFile
import io.prediction.commons.scalding.appdata.ItemsSource
import io.prediction.commons.scalding.appdata.ItemsSource.FIELD_SYMBOLS
import io.prediction.commons.appdata.{ Item }

/**
 * File Format:
 * <id>\t<itypes>\t<appid>\t<starttime>\t<ct>\t<endtime>
 *
 * endtime is optional
 * use PIO_NONE if no value for optional field
 *
 * Example:
 * 1  t1,t2,t3  4  123456  123210  654321
 * 1  t1,t2,t3  4  123456  123210  PIO_NONE
 */
class FileItemsSource(path: String, appId: Int, itypes: Option[List[String]]) extends Tsv(
  p = path + "items.tsv"
) with ItemsSource {

  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL

  override def getSource: Source = this

  override def readData(iidField: Symbol, itypesField: Symbol)(implicit fd: FlowDef): Pipe = {
    this.read
      .mapTo((0, 1) -> (iidField, itypesField)) { fields: (String, String) =>
        val (iid, itypes) = fields

        (iid, itypes.split(",").toList)

      }.then(filterItypes(itypesField, itypes) _)

  }

  private def filterItypes(itypesField: Symbol, queryItypes: Option[List[String]])(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dataPipe =
      if (queryItypes == None) p
      else p.filter(itypesField) { x: List[String] =>
        val orgSize = x.size
        val diffList = x diff queryItypes.get
        // diff return a new list WITHOUT element appearing in queryItypes.
        // if diffList is shorter than original, it means itypes has elements in queryItypes
        // if diffList is the same, it means itypes has no element in queryItypes.
        // Since we want items which is one of queryItypes, we only want item has diffList smaller.
        diffList.size < orgSize
      }

    dataPipe
  }

  override def readStartEndtime(iidField: Symbol, itypesField: Symbol, starttimeField: Symbol, endtimeField: Symbol)(implicit fd: FlowDef): Pipe = {
    this.read
      .mapTo((0, 1, 3, 5) -> (iidField, itypesField, starttimeField, endtimeField)) { fields: (String, String, Long, String) =>
        val (iid, itypes, starttime, endtime) = fields

        val endtimeOpt: Option[Long] = endtime match {
          case "PIO_NONE" => None
          case x: String => {
            try {
              Some(x.toLong)
            } catch {
              case e: Exception => {
                assert(false, s"Failed to convert ${x} to Long. Exception: " + e)
                Some(0)
              }
            }
          }
        }

        (iid, itypes.split(",").toList, starttime, endtimeOpt)

      }.then(filterItypes('itypes, itypes) _)
  }

  override def readObj(objField: Symbol)(implicit fd: FlowDef): Pipe = {
    val items = this.read
      .mapTo((0, 1, 2, 3, 4) -> (objField, 'itypes)) { fields: (String, String, Int, Long, Long) =>
        val (id, itypes, appid, starttime, ct) = fields

        val itypesList = itypes.split(",").toList

        (Item(
          id = id,
          appid = appid,
          ct = new DateTime(ct),
          itypes = itypesList,
          starttime = Some(new DateTime(starttime)),
          endtime = None,
          price = None,
          profit = None,
          latlng = None,
          inactive = None,
          attributes = None
        ), itypesList)

      }
      .then(filterItypes('itypes, itypes) _)
      .project(objField)

    items
  }

  override def writeData(iidField: Symbol, itypesField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo((iidField, itypesField) ->
      (FIELD_SYMBOLS("id"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("appid"))) {
      fields: (String, List[String]) =>
        val (iid, itypes) = fields

        (iid, itypes.mkString(","), appid)
    }.write(this)

    writtenData
  }

  override def writeObj(objField: Symbol)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo(objField ->
      (FIELD_SYMBOLS("id"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("appid"), FIELD_SYMBOLS("starttime"), FIELD_SYMBOLS("ct"))) { obj: Item =>

      val starttime: java.util.Date = obj.starttime.get.toDate()
      val ct: java.util.Date = obj.ct.toDate()

      (obj.id, obj.itypes.mkString(","), obj.appid, starttime.getTime(), ct.getTime())

    }.write(this)

    writtenData
  }

}