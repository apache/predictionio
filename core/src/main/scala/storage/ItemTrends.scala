package io.prediction.storage

import com.github.nscala_time.time.Imports._
import org.json4s._

package object stock {
  type DailyTuple = Tuple8[DateTime, Double, Double, Double, Double, Double, Double, Boolean]
}

/**
 * ItemTrend object.
 *
 * For now, there is only one time series attached to an ItemTrend object. We
 * expect to add more, e.g. active, volume, etc..
 *
 * @param id ID.
 * @param appid App ID that this itemTrend belongs to.
 * @param ct Creation time.
 * @param basetime Base time for the time series
 * @param prices A time series for price.
 * @param actives A time series for activeness. Set to false if field is
 * missing.
 */
case class ItemTrend(

  id: String,
  appid: Int,
  ct: DateTime,
  //priceRaw: Seq[(DateTime, Double)] = List[(DateTime, Double)](),

  // Date, Open, High Low, Close, Volume, Adj.Close, Active
  // The active field is true if data originally not exists.
  daily: Seq[stock.DailyTuple] = Vector[stock.DailyTuple]())

/**
 * Base trait for implementations that interact with itemTrendTrends in the
 * backend app data store.
 */
trait ItemTrends {
  /** Insert a new itemTrendTrend. */
  def insert(itemTrendTrend: ItemTrend): Unit

  /** Get an itemTrendTrend by ID. */
  def get(appid: Int, id: String): Option[ItemTrend]

  /** Check if itemTrendTrend by ID exists. */
  def contains(appid: Int, id: String): Boolean

  /** Find all itemTrendTrends by App ID. */
  def getByAppid(appid: Int): Iterator[ItemTrend]

  /** Get itemTrendTrends by IDs. */
  def getByIds(appid: Int, ids: Seq[String]): Seq[ItemTrend]

  /** Update an itemTrend. */
  //def update(itemTrend: ItemTrend): Unit

  /*
  def appendPrice(appid: Int, id: String, t: DateTime, p: Double): Unit

  def extendPrice(appid: Int, id: String, newPrice: Seq[(DateTime, Double)]): Unit
  */

  /** Delete an itemTrend. */
  def delete(appid: Int, id: String): Unit

  /** Delete an itemTrend. */
  def delete(itemTrend: ItemTrend): Unit

  /** Delete all itemTrendTrends by App ID */
  def deleteByAppid(appid: Int): Unit

  /** count number of records by App ID*/
  def countByAppid(appid: Int): Long
}

class ItemTrendSerializer extends CustomSerializer[ItemTrend](formats => (
  {
    case JObject(fields) =>
      val seed = ItemTrend(
        id = "",
        appid = 0,
        ct = DateTime.now)
      fields.foldLeft(seed) { case (itemtrend, field) =>
        field match {
          case JField("id", JString(id)) => itemtrend.copy(id = id)
          case JField("appid", JInt(appid)) => itemtrend.copy(
            appid = appid.intValue)
          case JField("ct", JString(ct)) => itemtrend.copy(
            ct = Utils.stringToDateTime(ct))
          case JField("daily", JArray(d)) => itemtrend.copy(daily =
            d map { _ match {
              case JObject(List(
                JField("date", JString(date)),
                JField("open", JDouble(open)),
                JField("high", JDouble(high)),
                JField("low", JDouble(low)),
                JField("close", JDouble(close)),
                JField("volume", JDouble(volume)),
                JField("adjclose", JDouble(adjclose)),
                JField("active", JBool(active)))) => new stock.DailyTuple(
                Utils.stringToDateTime(date),
                open,
                high,
                low,
                close,
                volume,
                adjclose,
                active)
              }
            }
          )
        }
      }
  },
  {
    case itemtrend: ItemTrend =>
      JObject(
        JField("id", JString(itemtrend.id)) ::
        JField("appid", JInt(itemtrend.appid)) ::
        JField("ct", JString(itemtrend.ct.toString)) ::
        JField("daily", JArray(itemtrend.daily.map(d =>
          JObject(List(
            JField("date", JString(d._1.toString)),
            JField("open", JDouble(d._2)),
            JField("high", JDouble(d._3)),
            JField("low", JDouble(d._4)),
            JField("close", JDouble(d._5)),
            JField("volume", JDouble(d._6)),
            JField("adjclose", JDouble(d._7)),
            JField("active", JBool(d._8))))).toList)) :: Nil)
  }
))
