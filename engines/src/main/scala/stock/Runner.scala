package io.prediction.engines.stock
import com.github.nscala_time.time.Imports.DateTime

import io.prediction.core.BaseEngine
//import io.prediction.core.AbstractEngine
import io.prediction.DefaultServer
import io.prediction.DefaultCleanser
import io.prediction.SparkDefaultCleanser

import io.prediction.workflow.SparkWorkflow
//import io.prediction.workflow.EvaluationWorkflow

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

// Ugly workaround since generic types require Manifest which imposes a non
// empty constructor....

object Runner {
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

    //val validationParams = new ValidationParams(pThreshold = 0.01)

    val serverParams = new StockServerParams(i = 0)

    val engine = StockEngine()

    //val evaluator = StockEvaluator()
    //val evaluator = SparkStockEvaluator()


    val evaluator = BackTestingEvaluator()

    val validationParams = new BackTestingParams(0.001, 0.00)
    //val validationParams = null

    val algoParamsSet = Seq(
      ("regression", null),
      ("random", new RandomAlgoParams(seed = 1, scale = 0.02)),
      ("random", new RandomAlgoParams(seed = 2, drift = 1))
    )

    /*
    if (false) {
      // PIO runner
      val evalWorkflow = EvaluationWorkflow(
        "", 
        evalDataParams, 
        null,  // validationParams 
        null,  // cleanserParams 
        algoParamsSet, 
        serverParams,
        engine,
        evaluator)

      println("Start singlethread runner")
      evalWorkflow.run
    }
    */

    if (true) {
      // PIO runner
      SparkWorkflow.run(
        "Iron Man", 
        evalDataParams, 
        validationParams /* validation */, 
        null /* cleanserParams */, 
        algoParamsSet, 
        serverParams,
        engine,
        /*
        //engine.cleanserClass,
        classOf[NoOptCleanser],
        Map("random" -> classOf[RandomAlgorithm],
          "regression" -> classOf[RegressionAlgorithm]),
        classOf[StockServer],
        */
        evaluator)
      println("--------------- RUN LOCAL ------------------")
    }
  }
}
