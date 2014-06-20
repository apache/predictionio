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
  def apply() = {
    new SparkEvaluator(
      classOf[SparkStockDataPreparator],
      classOf[StockValidator])
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
    val market = td.data._2.find(_._1 == "SPY").map(_._2).get
    new SparkTrainingData(
      timeIndex = td.data._1,
      tickers = td.tickers,
      mktTicker = td.mktTicker,
      market = market,
      price = sc.parallelize(td.data._2))
  }

  def prepareValidation(sc: SparkContext, params: ValidationDataParams)
  : RDD[(Feature, Target)] = {
    sc.parallelize(dataPrep.prepareValidation(params))
  }
}


class LabeledPointMaker(
  val timeIndex: Array[DateTime],
  val market: Array[Double]) extends Serializable {
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
  //val mktPrice: Series[Int, Double] = Series(market)
  val index = IndexTime(timeIndex:_ *)
  val mktLgPrice = Series(Vec(market), index).mapValues(math.log)

  def makeLabeledPoints(data: (String, Array[Double])): Seq[LabeledPoint] = {
    //val (ticker, price) = data
    val ticker = data._1
    val lgPrice = Series(Vec(data._2), index).mapValues(math.log)
    val firstIdx = 30
    val lastIdx = lgPrice.length - 1
    val timeLength = mktLgPrice.length
    val points = ArrayBuffer[LabeledPoint]()
      
    //val lgPrice = price.mapValues(math.log)
      
    val fwdRet = new ReturnFeature(-1).make(lgPrice)

    val featureVecs = featureMakerSeq.map{ maker => {
      val vector = maker.make(lgPrice, mktLgPrice)
      assert(vector.length == timeLength, 
        s"${maker} mismatch len. " + 
        s"t: $ticker a: ${vector.length} e: $timeLength")
      vector
    }}

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
}

class StockTreeModel(val treeModel: DecisionTreeModel) extends BaseModel

//class StockTreeModel extends DecisionTreeModel with BaseModel

class SparkTreeAlgorithm
    extends SparkAlgorithm[
        SparkTrainingData,
        Feature,
        Target,
        StockTreeModel,
        EmptyParams] {
  def init(p: EmptyParams) = {}

  def train(data: SparkTrainingData): StockTreeModel = { 
    val maker = new LabeledPointMaker(data.timeIndex, data.market)
    val points = data.price.flatMap(maker.makeLabeledPoints)
    
    val maxDepth = 5
    val strategy = new Strategy(
      algo = Regression, 
      impurity = Variance, 
      maxDepth = maxDepth//,
      //maxBins = 10,
    )
    val treeModel = DecisionTree.train(points, strategy)
    //model.asInstanceOf[StockTreeModel]
    new StockTreeModel(treeModel)
  }

  def predict(model: StockTreeModel, feature: Feature): Target = {
    //model.treeModel.predict(feature)
    null
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
        /*
        classOf[SparkNoOptCleanser],
        Map("random" -> classOf[RandomAlgorithm],
          "regression" -> classOf[RegressionAlgorithm]),
        */
        evaluator)
        
      println("--------------- RUN SPARK ------------------")
    }
  }


}

