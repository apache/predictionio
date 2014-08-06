package io.prediction.examples.stock

import io.prediction.storage.Storage
import io.prediction.storage.{ ItemTrend, ItemTrends }
import grizzled.slf4j.Logger
import scala.io.Source

import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import scala.collection.mutable.ArrayBuffer

/*
http://ichart.finance.yahoo.com/table.csv?
s=GOOG&d=4&e=7&f=2014&g=d&a=2&b=27&c=2014&ignore=.csv
*/

class YahooFetcher(appid: Int, fromYear: Int, fromMonth: Int, fromDay: Int,
    toYear: Int, toMonth: Int, toDay: Int) {
  val logger = Logger(YahooFetcher.getClass)
  // yahoo.controller.use 0-base for month
  val fromMonth_ = fromMonth - 1
  val toMonth_ = toMonth - 1

  val marketTicker = "SPY"

  // base time
  val baseTime = fetchRaw(marketTicker).keys.toSeq.sorted

  def getUrl(ticker: String) = {
    (f"http://ichart.finance.yahoo.com/table.csv?" +
      f"s=$ticker&a=${fromMonth_}&b=$fromDay&c=$fromYear&" +
      f"d=${toMonth_}&e=${toDay}&f=$toYear")
  }

  def fetchRaw(ticker: String) = {
    //println(getUrl(ticker))
    val t = Source.fromURL(getUrl(ticker)).mkString
    val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss zzz")
    val yahooRaw = t.split("\n").drop(1).map(_.split(",")).map { l =>
      {
        // Fix date
        val dateStr = l(0) + " 16:00:00 EST"
        val dt = dateFormatter.parseDateTime(dateStr)
        (dt, (
          l(1).toDouble, l(2).toDouble, l(3).toDouble, l(4).toDouble,
          l(5).toDouble, l(6).toDouble))
      }
    }.toMap
    yahooRaw
  }

  def fillNA(
    raw: Map[DateTime, (Double, Double, Double, Double, Double, Double)]) = {
    val data = ArrayBuffer[(DateTime, Double, Double, Double, Double, Double, Double, Boolean)]()

    for (t <- baseTime) {
      data.append(raw.get(t).map(
        e => (t, e._1, e._2, e._3, e._4, e._5, e._6, true)
      ).getOrElse(
          (t, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
        ))
    }

    // Construct aprc series
    val aprcBuffer = ArrayBuffer[Double](100.0)

    // This handles data missing from the starts. data with gaps are not
    // supported.
    for (i <- 1 until data.length) {
      val prevActive = data(i - 1)._8
      val currActive = data(i)._8
      val aret = (if (prevActive && currActive) {
        (data(i)._7 / data(i - 1)._7) - 1
      } else {
        0.0
      })
      val currAprc = aprcBuffer(i - 1) * (1 + aret)
      aprcBuffer.append(currAprc)
    }

    (0 until data.length).map(i => {
      val d = data(i)
      val aprc = aprcBuffer(i)
      (d._1, d._2, d._3, d._4, d._5, d._6, aprc, d._8)
    })
  }

  def fetchTicker(ticker: String) = {
    logger.info(s"Fetching $ticker")
    val rawData = fetchRaw(ticker)
    val cleansedData = fillNA(rawData)
    ItemTrend(
      id = ticker,
      appid = appid,
      ct = DateTime.now,
      daily = cleansedData)
  }

}

object YahooFetcher {
  def apply(appid: Int, fromYear: Int, fromMonth: Int, fromDay: Int,
    toYear: Int, toMonth: Int, toDay: Int) = {
    new YahooFetcher(appid, fromYear, fromMonth, fromDay,
      toYear, toMonth, toDay)
  }
}

object FetchMain {
  val logger = Logger(FetchMain.getClass)

  //val appid = PIOStorage.appid
  //val appid = Settings.appid
  val itemTrendsDb = Storage.getAppdataItemTrends()
  val itemTrendsDbGetTicker = itemTrendsDb.get(Settings.appid, _: String).get

  def main(args: Array[String]) {
    logger.info(s"Download to appid: ${Settings.appid}")
    val fetcher = YahooFetcher(Settings.appid, 1998, 1, 1, 2015, 1, 1)

    (Settings.sp500List ++ Settings.marketList).foreach(ticker => {
      try {
        val itemTrend = fetcher.fetchTicker(ticker)
        itemTrendsDb.insert(itemTrend)
      } catch {
        case e: java.io.FileNotFoundException =>
          logger.error(s"${e.getMessage}")
      }
    })

  }
}
