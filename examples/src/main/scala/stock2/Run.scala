package io.prediction.examples.stock2

import io.prediction.controller.Workflow
import io.prediction.controller.WorkflowParams
import io.prediction.controller.IdentityPreparator
import io.prediction.controller.EmptyParams
import io.prediction.controller.FirstServing
import io.prediction.controller.Params
import com.github.nscala_time.time.Imports._
import scala.collection.immutable.HashMap
import java.io.File

// Buy if l-days daily return is high than s-days daily-return
case class MomentumStrategyParams(val l: Int, val s: Int) extends Params

class MomentumStrategy(val p: MomentumStrategyParams)
  extends StockStrategy[AnyRef] {
  
  def createModel(dataView: DataView): AnyRef = None

  def onClose(model: AnyRef, query: Query): Prediction = {
    val dataView = query.dataView

    val priceFrame = dataView.priceFrame(p.l + 1)
    val todayLgPrice = priceFrame.rowAt(p.l).mapValues(math.log)
    val lLgPrice = priceFrame.rowAt(0).mapValues(math.log)
    val sLgPrice = priceFrame.rowAt(p.l - p.s).mapValues(math.log)

    val sLgRet = (todayLgPrice - sLgPrice) / p.s
    val lLgRet = (todayLgPrice - lLgPrice) / p.l

    val output = query.tickers
    .map { ticker => {
      val s = sLgRet.first(ticker)
      val l = lLgRet.first(ticker)
      val p = l - s
      (ticker, p)
    }}

    Prediction(data = HashMap(output:_*))
  }
}


object Run {
  val tickerList = Seq(
      "GOOG", "GOOGL", "FB", "AAPL", "AMZN", "MSFT", "IBM", "HPQ", "INTC",
      "NTAP", "CSCO", "ORCL", "XRX", "YHOO", "AMAT", "QCOM", "TXN", "CRM",
      "INTU", "WDC", "SNDK")

  def main(args: Array[String]) {
    val dataSourceParams = (if (true) {
        new DataSourceParams(
          baseDate = new DateTime(2002, 1, 1, 0, 0),
          fromIdx = 300,
          untilIdx = 400,
          trainingWindowSize = 200,
          maxTestingWindowSize = 20,
          marketTicker = "SPY",
          tickerList = tickerList)
      } else {
        // Need to pass "--driver-memory 8G" to pio-run since it requires a lot
        // of driver memory.
        new DataSourceParams(
          baseDate = new DateTime(2002, 1, 1, 0, 0),
          fromIdx = 300,
          untilIdx = 2000,
          trainingWindowSize = 200,
          maxTestingWindowSize = 20,
          marketTicker = "SPY",
          tickerList = io.prediction.examples.stock.Settings.sp500List)
      })

    val momentumParams = MomentumStrategyParams(20, 3)

    val metricsParams = BacktestingParams(
      enterThreshold = 0.01, 
      exitThreshold = 0.0, 
      maxPositions = 10,
      optOutputPath = Some(new File("metrics_results").getCanonicalPath)
    )

    Workflow.run(
      dataSourceClassOpt = Some(classOf[DataSource]),
      dataSourceParams = dataSourceParams,
      preparatorClassOpt = Some(IdentityPreparator(classOf[DataSource])),
      algorithmClassMapOpt = Some(Map(
        "" -> classOf[MomentumStrategy]
      )),
      algorithmParamsList = Seq(("", momentumParams)),
      servingClassOpt = Some(FirstServing(classOf[EmptyStrategy])),
      metricsClassOpt = Some(classOf[BacktestingMetrics]),
      metricsParams = metricsParams,
      params = WorkflowParams(
        verbose = 0,
        batch = "Imagine: Stock II"))
  }
}


