package io.prediction.engines.stock

import com.github.nscala_time.time.Imports.DateTime

import io.prediction.core.BaseEngine
import io.prediction.DefaultServer

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import io.prediction.workflow.SparkWorkflow
import io.prediction.core.SparkDataPreparator
//import io.prediction.core.SparkEvaluator
//import io.prediction.core.SparkEngine
import io.prediction.core.Spark2LocalAlgorithm
import io.prediction._
import io.prediction.core.BaseEvaluator

import org.apache.spark.mllib.tree.model.DecisionTreeModel
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.configuration.Algo._
import org.apache.spark.mllib.tree.configuration.Strategy
import org.apache.spark.mllib.tree.configuration.FeatureType
import org.apache.spark.mllib.tree.impurity.Gini
import org.apache.spark.mllib.tree.impurity.Variance
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import scala.collection.mutable.ArrayBuffer

import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._

/******************** Factories ***********************************/
object SparkStockEvaluator extends EvaluatorFactory {
  def apply() = {
    //new SparkEvaluator(
    new BaseEvaluator(
      classOf[SparkStockDataPreparator],
      classOf[StockValidator])
  }
}

object SparkBackTestingEvaluator extends EvaluatorFactory {
  def apply() = {
    new BaseEvaluator(
    //new SparkEvaluator(
      classOf[SparkStockDataPreparator],
      classOf[BackTestingValidator])
  }
}

object SparkStockEngine extends EngineFactory {
  def apply() = {
    //new SparkEngine(
    new BaseEngine(
      classOf[SparkNoOptCleanser],
      Map("tree" -> classOf[SparkTreeAlgorithm]),
      classOf[DefaultServer[Feature, Target]])
  }
}

/******************** Data ***********************************/
// For spark algo, it requires spark-specific TrainingData and Model
class SparkTrainingData (
  val timeIndex: Array[DateTime],  
  val tickers: Seq[String],
  val mktTicker: String,
  val market: Array[Double],
  // price doesn't contain market data, as these are RDD and are operated in
  // parallel.
  val price: RDD[(String, Array[Double])]
) extends Serializable

class StockTreeModel(val treeModel: DecisionTreeModel) extends Serializable

/******************** Controllers ***********************************/
class SparkNoOptCleanser extends SparkDefaultCleanser[SparkTrainingData] {}

// Data Preparator. Mainly based on local-data-prep, but export data in RDD.
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
    val priceArray: Array[(String, Array[Double])] = td.data._2
    val mktTicker = params.marketTicker

    val market = priceArray.find(_._1 == mktTicker).map(_._2).get
    val price = priceArray.filter(_._1 != mktTicker)

    new SparkTrainingData(
      timeIndex = td.data._1,
      tickers = td.tickers,
      mktTicker = mktTicker,
      market = market,
      price = sc.parallelize(price))
  }

  def prepareValidation(sc: SparkContext, params: ValidationDataParams)
  : RDD[(Feature, Target)] = {
    sc.parallelize(dataPrep.prepareValidation(params))
  }
}

// Spark Algo. Based on MLLib's decision tree
class SparkTreeAlgorithm
  extends Spark2LocalAlgorithm[
      SparkTrainingData,
      Feature,
      Target,
      StockTreeModel,
      EmptyParams] {
  //def init(p: EmptyParams) = {}

  def train(data: SparkTrainingData): StockTreeModel = { 
    val timeIndex = IndexTime(data.timeIndex)
    val market = Series(Vec(data.market), index=timeIndex)
    val price = data.price
    val maker = LabeledPointMaker(market)

    val points = price.flatMap(maker.makeLabeledPoints)
    
    val maxDepth = 5
    val strategy = new Strategy(
      algo = Regression, 
      impurity = Variance, 
      maxDepth = maxDepth
    )
    val treeModel = DecisionTree.train(points, strategy)

    println(s"Time: $timeIndex")
    DecisionTreePrinter.p(treeModel)(maker.featureMakerSeq)

    new StockTreeModel(treeModel)
  }

  def predict(model: StockTreeModel, feature: Feature): Target = {
    val treeModel = model.treeModel
    
    val index = feature.data.rowIx
    val priceFrame = feature.data
    val market = priceFrame.firstCol(feature.mktTicker)

    val maker = LabeledPointMaker(market)

    val length = index.length

    println(s"Prediction At ${feature.timeIndex.last}")

    val prediction = feature.tickerList
    .map{ ticker => {
      val lgPrice: Series[DateTime, Double] = priceFrame
        .firstCol(ticker)
        .mapValues(math.log)

      val featureVecs = maker.makeFeatureVectors(ticker, lgPrice)

      val feature = featureVecs.map(_.raw(length - 1)).toArray
      val p = treeModel.predict(Vectors.dense(feature))
      println(s"predict: $ticker $p")

      (ticker, p)
    }}
    .toMap

    new Target(data = prediction)
  }
}



object RunSpark {
  /*
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
    "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
    "XRX", "YHOO", "AMAT", "QCOM", "TXN",
    "CRM", "INTU", "WDC", "SNDK")
  */

  val tickerList = Seq("GOOG", "MSFT")

  def main(args: Array[String]) {
    val evalDataParams = new EvaluationDataParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      untilIdx = 630,
      //untilIdx = 800,
      //untilIdx = 1200,
      trainingWindowSize = 600,
      evaluationInterval = 20,
      marketTicker = "SPY",
      tickerList = tickerList)

    //val validationParams = new ValidationParams(pThreshold = 0.01)

    //val serverParams = new StockServerParams(i = 0)
    val serverParams = null

    val engine = SparkStockEngine()

    //val evaluator = SparkStockEvaluator()
    val evaluator = SparkBackTestingEvaluator()
    val validationParams = new BackTestingParams(0.003, 0.00)


    //val validationParams = null

    val algoParamsSet = Seq(
      //("regression", null),
      //("random", new RandomAlgoParams(seed = 1, scale = 0.02)),
      //("random", new RandomAlgoParams(seed = 2, drift = 1))
      ("tree", null)
    )

    if (true) {
      // PIO runner
      SparkWorkflow.run(
        "Iron Man", 
        evalDataParams, 
        validationParams,
        null,
        algoParamsSet, 
        serverParams,
        engine,
        evaluator)
        
      println("--------------- RUN SPARK ------------------")
    }

    System.exit(0)
  }


}

