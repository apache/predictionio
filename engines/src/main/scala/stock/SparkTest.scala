package io.prediction.engines.stock

import com.github.nscala_time.time.Imports.DateTime

import io.prediction.core.BaseEngine
//import io.prediction.core.AbstractEngine
import io.prediction.DefaultServer
//import io.prediction.DefaultCleanser

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import io.prediction.workflow.SparkWorkflow
import io.prediction.core.SparkDataPreparator
import io.prediction.core.SparkEvaluator
import io.prediction._

/*
// In this case, TD may contain multiple RDDs
// But still, F and A cannot contain RDD
abstract class SparkDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams : Manifest,
    VDP <: BaseValidationDataParams,
    TD : Manifest,
    F <: BaseFeature,
    A <: BaseActual]
    extends BaseDataPreparator[EDP, TDP, VDP, RDD[TD], F, A] {
  def prepareTrainingBase(
    sc: SparkContext,
    params: BaseTrainingDataParams): TD = {
    println("SparkDataPreparator.prepareTrainingBase")
    val tdp = params.asInstanceOf[TDP]
    prepareTraining(sc, tdp)
  }

  def prepareTraining(sc: SparkContext, params: TDP): TD

  def prepareValidationBase(
    sc: SparkContext,
    params: BaseValidationDataParams): RDD[(F, A)] = {
    val vdp = params.asInstanceOf[VDP]
    prepareValidation(sc, vdp)
  }
  
  def prepareValidation(sc: SparkContext, params: VDP): RDD[(F, A)]
}
*/

object SparkStockEvaluator extends EvaluatorFactory {
  // Use singleton class here to avoid re-registering hooks in config.
  //val config = new Config()
  //val itemTrendsDb = config.getAppdataItemTrends()

  def apply() = {
    //new BaseEvaluator(
    new SparkEvaluator(
      classOf[SparkStockDataPreparator],
      classOf[StockValidator])
  }
}

class SparkNoOptCleanser extends SparkDefaultCleanser[SparkTrainingData] {}

class SparkTrainingData (
  val timeIndex: Array[DateTime],  
  val tickers: Seq[String],
  val mktTicker: String,
  val price: RDD[(String, Array[Double])]
) extends BaseTrainingData

class SparkStockDataPreparator
    extends SparkDataPreparator[
        EvaluationDataParams, 
        TrainingDataParams, 
        ValidationDataParams,
        SparkTrainingData, 
        Feature, 
        Target] {
  val dataPrep = new StockDataPreparator

  def getParamsSet(p: EvaluationDataParams) = dataPrep.getParamsSet(p)

  def prepareTraining(sc: SparkContext, params: TrainingDataParams)
  : SparkTrainingData = {
    val localData: Array[String] = params.tickerList.toArray
    val td = dataPrep.prepareTraining(params)
    new SparkTrainingData(
      timeIndex = td.data._1,
      tickers = td.tickers,
      mktTicker = td.mktTicker,
      price = sc.parallelize(td.data._2))
  }

  def prepareValidation(sc: SparkContext, params: ValidationDataParams)
  : RDD[(Feature, Target)] = {
    sc.parallelize(dataPrep.prepareValidation(params))
  }
}

object RunSpark {
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
    val evaluator = SparkStockEvaluator()


    //val evaluator = BackTestingEvaluator()

    //val validationParams = new BackTestingParams(0.003, 0.00)
    val validationParams = null

    val algoParamsSet = Seq(
      ("regression", null),
      ("random", new RandomAlgoParams(seed = 1, scale = 0.02)),
      ("random", new RandomAlgoParams(seed = 2, drift = 1))
    )

    /*   
    if (true) {
      // PIO runner
      SparkWorkflow.run(
        "Iron Man", 
        evalDataParams, 
        validationParams,
        null,
        algoParamsSet, 
        serverParams,
        //engine,
        classOf[SparkNoOptCleanser],
        Map("random" -> classOf[RandomAlgorithm],
          "regression" -> classOf[RegressionAlgorithm]),
        evaluator)
      println("--------------- RUN SPARK ------------------")
    }
    */
  }


}

