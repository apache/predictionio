package io.prediction.stock
import com.github.nscala_time.time.Imports.DateTime

import io.prediction.PIORunner

object Run {
  //val tickerList = Seq("GOOG", "AAPL", "FB")
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
      "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
      "XRX", "YHOO", "AMAT", "QCOM", "TXN",
      "CRM", "INTU", "WDC", "SNDK")

  def main(args: Array[String]) {
    val params = new EvaluationParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      untilIdx = 800,
      trainingWindowSize = 600,
      evaluationInterval = 20,
      marketTicker = "SPY",
      tickerList = tickerList)

    val dataPreparator = new DataPreparator
    val algorithm = new Algorithm
    val server = new Server
    val evaluator = new Evaluator
    PIORunner.run(params, dataPreparator, algorithm, server, evaluator)
  }
}
