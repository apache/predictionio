package io.prediction.engines.stock
import com.github.nscala_time.time.Imports.DateTime

import io.prediction.core.BaseEngine
import io.prediction.core.AbstractEngine
import io.prediction.DefaultServer
import io.prediction.DefaultCleanser

import io.prediction.workflow.SparkWorkflow
import io.prediction.workflow.EvaluationWorkflow

object Run {
  //val tickerList = Seq("GOOG", "AAPL", "FB", "GOOGL", "MSFT")
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
    "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
    "XRX", "YHOO", "AMAT", "QCOM", "TXN",
    "CRM", "INTU", "WDC", "SNDK")

  def main(args: Array[String]) {
    val evalDataParams = new EvaluationDataParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      untilIdx = 630,
      //untilIdx = 1200,
      trainingWindowSize = 600,
      evaluationInterval = 20,
      marketTicker = "SPY",
      tickerList = tickerList)

    val serverParams = new StockServerParams(i = 1)

    val engine = StockEngine()

    val evaluator = StockEvaluator()

    val algoParamsSet = Seq(
      ("random", new RandomAlgoParams(seed = 1, scale = 0.02)),
      ("random", new RandomAlgoParams(seed = 2, drift = 1)),
      ("regression", null)
    )

    if (false) {
      // PIO runner
      val evalWorkflow = EvaluationWorkflow(
        "", 
        evalDataParams, 
        null /* validationParams */,
        null /* cleanserParams */, 
        algoParamsSet, 
        serverParams,
        engine,
        evaluator)

      println("Start singlethread runner")
      evalWorkflow.run
    }
    
    if (true) {
      // PIO runner
      SparkWorkflow.run(
        "Iron Man", 
        evalDataParams, 
        null /* validation */, 
        null /* cleanserParams */, 
        algoParamsSet, 
        serverParams,
        engine,
        evaluator)
    }
  }
}
