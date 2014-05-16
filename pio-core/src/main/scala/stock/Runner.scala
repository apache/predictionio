package io.prediction.stock
import com.github.nscala_time.time.Imports.DateTime

import io.prediction.PIORunner
import io.prediction.BaseEngine
import io.prediction.DefaultServer

object Run {
  val tickerList = Seq("GOOG", "AAPL", "FB", "GOOGL", "MSFT")
  /*
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
      "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
      "XRX", "YHOO", "AMAT", "QCOM", "TXN",
      "CRM", "INTU", "WDC", "SNDK")
  */

  def main(args: Array[String]) {
    val evalParams = new EvaluationParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      untilIdx = 650,
      trainingWindowSize = 600,
      evaluationInterval = 20,
      marketTicker = "SPY",
      tickerList = tickerList)


    val algoParams = new RandomAlgoParams(seed = 1, scale = 0.01)
    
    val engine = new BaseEngine(
      classOf[Preparator], 
      classOf[RandomAlgorithm], 
      classOf[DefaultServer[Feature, Target]])

    val evaluator = new Evaluator
    val preparator = new Preparator

    PIORunner.run(evalParams, algoParams, engine, evaluator, preparator)
  }
}
