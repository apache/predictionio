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
import io.prediction.core.SparkEvaluator
import io.prediction.core.SparkEngine
import io.prediction.core.SparkAlgorithm
import io.prediction._

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

object SparkStockEvaluator extends EvaluatorFactory {
  def apply() = {
    new SparkEvaluator(
      classOf[SparkStockDataPreparator],
      classOf[StockValidator])
  }
}

object SparkBackTestingEvaluator extends EvaluatorFactory {
  def apply() = {
    new SparkEvaluator(
      classOf[SparkStockDataPreparator],
      classOf[BackTestingValidator])
  }
}

class SparkNoOptCleanser extends SparkDefaultCleanser[SparkTrainingData] {}


object SparkStockEngine extends EngineFactory {
  def apply() = {
    new SparkEngine(
      classOf[SparkNoOptCleanser],
      Map("tree" -> classOf[SparkTreeAlgorithm]),
      classOf[DefaultServer[Feature, Target]])
  }
}

//class SparkNoOptCleanser extends SparkDefaultCleanser[SparkTrainingData] {}

class SparkTrainingData (
  val timeIndex: Array[DateTime],  
  val tickers: Seq[String],
  val mktTicker: String,
  val market: Array[Double],
  // price doesn't contain market data, as these are RDD and are operated in
  // parallel.
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

object LabeledPointMaker {
  def apply(market: Series[DateTime, Double]): LabeledPointMaker = {
    new LabeledPointMaker(market.index.toVec.contents, market.toVec.contents)
  }

  def apply(timeArray: Array[DateTime], marketArray: Array[Double])
  : LabeledPointMaker = {
    new LabeledPointMaker(timeArray, marketArray)
  }
}


class LabeledPointMaker(
  val timeArray: Array[DateTime],
  val marketArray: Array[Double]
  ) extends Serializable {
  @transient lazy val market: Series[DateTime, Double] =
      Series(Vec(marketArray), IndexTime(Vec(timeArray)))

  val featureMakerSeq = Seq(
    new ReturnFeature(5),
    new ReturnFeature(22),
    new MAFeature(5),
    new MAFeature(22),
    new MAReturnFeature(5),
    new MAReturnFeature(22),
    new MktReturnFeature(1),
    new MktReturnFeature(5),
    new MktReturnFeature(22),
    new ReturnFeature(1)
  )
  @transient lazy val mktLgPrice = market.mapValues(math.log)
  @transient lazy val timeLength = mktLgPrice.length

  def makeLabeledPoints(data: (String, Array[Double]))
  : Seq[LabeledPoint] = {
    val ticker = data._1
    val lgPrice = Series(Vec(data._2), market.index).mapValues(math.log)
    val firstIdx = 30
    val lastIdx = lgPrice.length - 1
    val points = ArrayBuffer[LabeledPoint]()
      
    val fwdRet = new ReturnFeature(-1).make(lgPrice)

    val featureVecs = makeFeatureVectors(ticker, lgPrice)

    (firstIdx until lastIdx).map( i => {
      val feature = featureVecs.map(_.raw(i)).toArray
      if (fwdRet.raw(i).isNaN) {
        println(s"$ticker ${lgPrice.rowIx.raw(i)} ${fwdRet.raw(i)}")
        assert(false)
      }
      val p = new LabeledPoint(fwdRet.raw(i), Vectors.dense(feature))
      points += p
    })

    points
  }

  def makeFeatureVectors(ticker: String, lgPrice: Series[DateTime, Double])
  : Seq[Series[DateTime, Double]] = {
    val featureVecs = featureMakerSeq.map{ maker => {
      val vector = maker.make(lgPrice, mktLgPrice)
      assert(vector.length == lgPrice.length, 
        s"${maker} mismatch len. " + 
        s"t: $ticker a: ${vector.length} e: ${lgPrice.length}")
      vector
    }}
    featureVecs
  }
}

class StockTreeModel(val treeModel: DecisionTreeModel) extends BaseModel

class SparkTreeAlgorithm
    extends SparkAlgorithm[
        SparkTrainingData,
        Feature,
        Target,
        StockTreeModel,
        EmptyParams] {
  def init(p: EmptyParams) = {}

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
      maxDepth = maxDepth//,
      //maxBins = 10,
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
      //untilIdx = 630,
      //untilIdx = 800,
      untilIdx = 1200,
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

