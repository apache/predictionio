package io.prediction.engines.stock2

import io.prediction.workflow.APIDebugWorkflow
import io.prediction.controller.IdentityPreparator
import io.prediction.controller.EmptyParams
import io.prediction.controller.FirstServing
import com.github.nscala_time.time.Imports._

object Run {
  /*
  val tickerList = Seq(
      "GOOG", "GOOGL", "FB", "AAPL", "AMZN", "MSFT", "IBM", "HPQ", "INTC",
      "NTAP", "CSCO", "ORCL", "XRX", "YHOO", "AMAT", "QCOM", "TXN", "CRM",
      "INTU", "WDC", "SNDK")
  */
  val tickerList = Seq(
      "GOOG", "GOOGL", "FB", "AAPL", "AMZN", "MSFT", "IBM", "HPQ", "INTC")

  def main(args: Array[String]) {
    val dataSourceParams = new DataSourceParams(
      baseDate = new DateTime(2011, 1, 1, 0, 0),
      fromIdx = 300,
      untilIdx = 350,
      trainingWindowSize = 200,
      maxTestingWindowSize = 20,
      marketTicker = "SPY",
      tickerList = tickerList)

    APIDebugWorkflow.run(
      dataSourceClassOpt = Some(classOf[DataSource]),
      dataSourceParams = dataSourceParams,
      preparatorClassOpt = Some(IdentityPreparator(classOf[DataSource])),
      algorithmClassMapOpt = Some(Map("" -> classOf[Strategy])),
      algorithmParamsList = Seq(("", EmptyParams())),
      servingClassOpt = Some(FirstServing(classOf[Strategy])),
      verbose = 3,
      batch = "Imagine: Stock II")
  }
}


