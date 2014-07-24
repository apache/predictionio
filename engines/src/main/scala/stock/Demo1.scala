package io.prediction.engines.stock

import io.prediction.controller._
import io.prediction.workflow.APIDebugWorkflow
import com.github.nscala_time.time.Imports.DateTime

object Demo1 {
  def main(args: Array[String]) {
    // Define Engine
    val dataSourceParams = new DataSourceParams(
        baseDate = new DateTime(2002, 1, 1, 0, 0),
        fromIdx = 600,
        untilIdx = 1200,
        trainingWindowSize = 300,
        evaluationInterval = 20,
        marketTicker = "SPY",
        tickerList = Seq("GOOGL"))

    val engine = RegressionEngineFactory()
    val engineParams = new SimpleEngineParams(dataSourceParams)

    // Define Metrics
    val metrics = Some(classOf[BacktestingMetrics])
    val backtestingParams = BacktestingParams(
      enterThreshold = 0.001, exitThreshold = 0.0)
    
    // Invoke workflow
    APIDebugWorkflow.runEngine(
      batch = "stock.Demo1", 
      verbose = 0,
      engine = engine,
      engineParams = engineParams,
      metricsClassOpt = metrics,
      metricsParams = backtestingParams)
  }
}

