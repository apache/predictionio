package io.prediction.examples.stock_old

import com.github.nscala_time.time.Imports.DateTime
import io.prediction.controller.Workflow
import io.prediction.controller.WorkflowParams
import io.prediction.controller.IdentityPreparator
import io.prediction.controller.EmptyParams
import io.prediction.controller.FirstServing

object Run {
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
    "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
    "XRX", "YHOO", "AMAT", "QCOM", "TXN",
    "CRM", "INTU", "WDC", "SNDK")

  def main(args: Array[String]) {
    val dataSourceParams = new DataSourceParams(
        baseDate = new DateTime(2002, 1, 1, 0, 0),
        fromIdx = 600,
        untilIdx = 900,
        trainingWindowSize = 300,
        evaluationInterval = 20,
        marketTicker = "SPY",
        tickerList = tickerList)

    //val algorithmParamsList = Seq(("Regression", EmptyParams()))

    val algorithmParamsList = Seq(
          ("Regression", EmptyParams()),
          ("Random", RandomAlgorithmParams(drift = 0.1)),
          ("Random", RandomAlgorithmParams(drift = -0.05)))

    Workflow.run(
        dataSourceClassOpt = Some(classOf[StockDataSource]),
        dataSourceParams = dataSourceParams,
        preparatorClassOpt = Some(IdentityPreparator(classOf[StockDataSource])),
        algorithmClassMapOpt = Some(Map(
          "Random" -> classOf[RandomAlgorithm],
          "Regression" -> classOf[RegressionAlgorithm])),
        algorithmParamsList = algorithmParamsList,
        servingClassOpt = Some(FirstServing(classOf[RegressionAlgorithm])),
        metricsClassOpt = Some(classOf[BacktestingMetrics]),
        metricsParams = BacktestingParams(0.002, 0.0),
        params = WorkflowParams(
          verbose = 0,
          batch = "Imagine: Stock"))
  }
}
