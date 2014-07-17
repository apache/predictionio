package io.prediction.engines.stock

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
  val sp500List = Vector(
    "A", "AA", "AAPL", "ABBV", "ABC", "ABT", "ACE", "ACN", "ACT", "ADBE", "ADI",
    "ADM", "ADP", "ADS", "ADSK", "ADT", "AEE", "AEP", "AES", "AET", "AFL",
    "AGN", "AIG", "AIV", "AIZ", "AKAM", "ALL", "ALLE", "ALTR", "ALXN", "AMAT",
    "AME", "AMGN", "AMP", "AMT", "AMZN", "AN", "AON", "APA", "APC", "APD",
    "APH", "ARG", "ATI", "AVB", "AVP", "AVY", "AXP", "AZO", "BA", "BAC", "BAX",
    "BBBY", "BBT", "BBY", "BCR", "BDX", "BEAM", "BEN", "BF-B", "BHI", "BIIB",
    "BK", "BLK", "BLL", "BMS", "BMY", "BRCM", "BRK-B", "BSX", "BTU", "BWA",
    "BXP", "C", "CA", "CAG", "CAH", "CAM", "CAT", "CB", "CBG", "CBS", "CCE",
    "CCI", "CCL", "CELG", "CERN", "CF", "CFN", "CHK", "CHRW", "CI", "CINF",
    "CL", "CLX", "CMA", "CMCSA", "CME", "CMG", "CMI", "CMS", "CNP", "CNX",
    "COF", "COG", "COH", "COL", "COP", "COST", "COV", "CPB", "CRM", "CSC",
    "CSCO", "CSX", "CTAS", "CTL", "CTSH", "CTXS", "CVC", "CVS", "CVX", "D",
    "DAL", "DD", "DE", "DFS", "DG", "DGX", "DHI", "DHR", "DIS", "DISCA", "DLPH",
    "DLTR", "DNB", "DNR", "DO", "DOV", "DOW", "DPS", "DRI", "DTE", "DTV", "DUK",
    "DVA", "DVN", "EA", "EBAY", "ECL", "ED", "EFX", "EIX", "EL", "EMC", "EMN",
    "EMR", "EOG", "EQR", "EQT", "ESRX", "ESS", "ESV", "ETFC", "ETN", "ETR",
    "EW", "EXC", "EXPD", "EXPE", "F", "FAST", "FB", "FCX", "FDO", "FDX", "FE",
    "FFIV", "FIS", "FISV", "FITB", "FLIR", "FLR", "FLS", "FMC", "FOSL", "FOXA",
    "FRX", "FSLR", "FTI", "FTR", "GAS", "GCI", "GD", "GE", "GGP", "GHC", "GILD",
    "GIS", "GLW", "GM", "GMCR", "GME", "GNW", "GOOG", "GOOGL", "GPC", "GPS",
    "GRMN", "GS", "GT", "GWW", "HAL", "HAR", "HAS", "HBAN", "HCBK", "HCN",
    "HCP", "HD", "HES", "HIG", "HOG", "HON", "HOT", "HP", "HPQ", "HRB", "HRL",
    "HRS", "HSP", "HST", "HSY", "HUM", "IBM", "ICE", "IFF", "IGT", "INTC",
    "INTU", "IP", "IPG", "IR", "IRM", "ISRG", "ITW", "IVZ", "JBL", "JCI", "JEC",
    "JNJ", "JNPR", "JOY", "JPM", "JWN", "K", "KEY", "KIM", "KLAC", "KMB", "KMI",
    "KMX", "KO", "KORS", "KR", "KRFT", "KSS", "KSU", "L", "LB", "LEG", "LEN",
    "LH", "LLL", "LLTC", "LLY", "LM", "LMT", "LNC", "LO", "LOW", "LRCX", "LSI",
    "LUK", "LUV", "LYB", "M", "MA", "MAC", "MAR", "MAS", "MAT", "MCD", "MCHP",
    "MCK", "MCO", "MDLZ", "MDT", "MET", "MHFI", "MHK", "MJN", "MKC", "MMC",
    "MMM", "MNST", "MO", "MON", "MOS", "MPC", "MRK", "MRO", "MS", "MSFT", "MSI",
    "MTB", "MU", "MUR", "MWV", "MYL", "NBL", "NBR", "NDAQ", "NE", "NEE", "NEM",
    "NFLX", "NFX", "NI", "NKE", "NLSN", "NOC", "NOV", "NRG", "NSC", "NTAP",
    "NTRS", "NU", "NUE", "NVDA", "NWL", "NWSA", "OI", "OKE", "OMC", "ORCL",
    "ORLY", "OXY", "PAYX", "PBCT", "PBI", "PCAR", "PCG", "PCL", "PCLN", "PCP",
    "PDCO", "PEG", "PEP", "PETM", "PFE", "PFG", "PG", "PGR", "PH", "PHM", "PKI",
    "PLD", "PLL", "PM", "PNC", "PNR", "PNW", "POM", "PPG", "PPL", "PRGO", "PRU",
    "PSA", "PSX", "PVH", "PWR", "PX", "PXD", "QCOM", "QEP", "R", "RAI", "RDC",
    "REGN", "RF", "RHI", "RHT", "RIG", "RL", "ROK", "ROP", "ROST", "RRC", "RSG",
    "RTN", "SBUX", "SCG", "SCHW", "SE", "SEE", "SHW", "SIAL", "SJM", "SLB",
    "SLM", "SNA", "SNDK", "SNI", "SO", "SPG", "SPLS", "SRCL", "SRE", "STI",
    "STJ", "STT", "STX", "STZ", "SWK", "SWN", "SWY", "SYK", "SYMC", "SYY", "T",
    "TAP", "TDC", "TE", "TEG", "TEL", "TGT", "THC", "TIF", "TJX", "TMK", "TMO",
    "TRIP", "TROW", "TRV", "TSCO", "TSN", "TSO", "TSS", "TWC", "TWX", "TXN",
    "TXT", "TYC", "UNH", "UNM", "UNP", "UPS", "URBN", "USB", "UTX", "V", "VAR",
    "VFC", "VIAB", "VLO", "VMC", "VNO", "VRSN", "VRTX", "VTR", "VZ", "WAG",
    "WAT", "WDC", "WEC", "WFC", "WFM", "WHR", "WIN", "WLP", "WM", "WMB", "WMT",
    "WU", "WY", "WYN", "WYNN", "X", "XEL", "XL", "XLNX", "XOM", "XRAY", "XRX",
    "XYL", "YHOO", "YUM", "ZION", "ZMH", "ZTS")
  val marketList = Vector("QQQ", "SPY")

  //val appid = PIOStorage.appid
  val appid = 42
  val itemTrendsDb = Storage.getAppdataItemTrends()
  val itemTrendsDbGetTicker = itemTrendsDb.get(appid, _: String).get

  def main(args: Array[String]) {
    logger.info(s"Download to appid: $appid")
    val fetcher = YahooFetcher(appid, 1998, 1, 1, 2015, 1, 1)

    (sp500List ++ marketList).foreach(ticker => {
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
