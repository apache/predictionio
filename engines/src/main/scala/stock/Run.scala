package io.prediction.engines.stock

import com.github.nscala_time.time.Imports.DateTime
import io.prediction.workflow.APIDebugWorkflow
import io.prediction.api.IdentityPreparator
import io.prediction.api.EmptyParams
import io.prediction.api.FirstServing

object Run {
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
    "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
    "XRX", "YHOO", "AMAT", "QCOM", "TXN",
    "CRM", "INTU", "WDC", "SNDK")

  def main(args: Array[String]) {
    val dataSourceParams = new DataSourceParams(
        baseDate = new DateTime(2006, 1, 1, 0, 0),
        fromIdx = 600,
        untilIdx = 800,
        trainingWindowSize = 600,
        evaluationInterval = 20,
        marketTicker = "SPY",
        tickerList = tickerList)
        
    //val algorithmParamsList = Seq(("Regression", EmptyParams()))

    val algorithmParamsList = Seq(
          ("Regression", EmptyParams()),
          ("Random", RandomAlgorithmParams(drift = 0.1)),
          ("Random", RandomAlgorithmParams(drift = -0.05)))

    APIDebugWorkflow.run(
        dataSourceClass = classOf[StockDataSource],
        dataSourceParams = dataSourceParams,
        preparatorClass = IdentityPreparator(classOf[StockDataSource]),
        algorithmClassMap = Map(
          "Random" -> classOf[RandomAlgorithm],
          "Regression" -> classOf[RegressionAlgorithm]),
        algorithmParamsList = algorithmParamsList,
        servingClass = FirstServing(classOf[RegressionAlgorithm]),
        metricsClass = classOf[BackTestingMetrics],
        metricsParams = BackTestingParams(0.002, 0.0),
        verbose = 0,
        batch = "Imagine: Stock") 
  }
}

