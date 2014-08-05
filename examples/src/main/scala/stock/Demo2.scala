package io.prediction.engines.stock

import io.prediction.controller._
import io.prediction.workflow.APIDebugWorkflow
import com.github.nscala_time.time.Imports.DateTime

object Demo2 {
  def main(args: Array[String]) {
    val tickerList = Seq(
      "GOOG", "GOOG", "FB", "AAPL", "AMZN", "MSFT", "IBM", "HPQ", "INTC",
      "NTAP", "CSCO", "ORCL", "XRX", "YHOO", "AMAT", "QCOM", "TXN", "CRM",
      "INTU", "WDC", "SNDK")

    // Define Engine
    val dataSourceParams = new DataSourceParams(
      baseDate = new DateTime(2004, 1, 1, 0, 0),
      fromIdx = 400,
      untilIdx = 1000,
      trainingWindowSize = 300,
      evaluationInterval = 20,
      marketTicker = "SPY",
      tickerList = tickerList)

    val engine = RegressionEngineFactory()
    val engineParams = new SimpleEngineParams(dataSourceParams)

    // Define Metrics
    val metrics = Some(classOf[BacktestingMetrics])
    val backtestingParams = BacktestingParams(
      enterThreshold = 0.001,
      exitThreshold = 0.0,
      maxPositions = 3)
    
    // Invoke workflow
    APIDebugWorkflow.runEngine(
      batch = "stock.Demo2", 
      verbose = 0,
      engine = engine,
      engineParams = engineParams,
      metricsClassOpt = metrics,
      metricsParams = backtestingParams)
  }
}
