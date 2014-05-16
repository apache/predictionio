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


    val preparator = new Preparator

    val algorithm = new Algorithm
    val algoParams = null

    //val algoParams = new RandomAlgoParams(seed = 1, scale = 0.01)
    //val algorithm = new RandomAlgorithm

    val server = new DefaultServer[Feature, Target]
    
    val engine = new BaseEngine(preparator, algorithm, server)

    val evaluator = new Evaluator
    PIORunner.run(evalParams, algoParams, engine, evaluator, preparator)
  }
}
