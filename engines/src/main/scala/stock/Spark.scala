package io.prediction.engines.stock

import io.prediction._
import com.github.nscala_time.time.Imports.DateTime
import scala.math

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.configuration.Algo._
import org.apache.spark.mllib.tree.configuration.Strategy
import org.apache.spark.mllib.tree.configuration.FeatureType
import org.apache.spark.mllib.tree.impurity.Gini
import org.apache.spark.mllib.tree.impurity.Variance


import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._

import scala.collection.mutable.ArrayBuffer
import org.saddle.stats.SeriesRollingStats

object Print {
  import org.apache.spark.mllib.tree.model._

  def p(model: DecisionTreeModel)(
    implicit featureMakerSeq: Seq[FeatureMaker]
    ): Unit = {
    println("Print model")
    p("", model.topNode)
  }

  def p(prefix: String, node: Node)(
    implicit featureMakerSeq: Seq[FeatureMaker]
    ): Unit = {
    if (node.isLeaf) {
      val stats = node.stats.get
      val stdev = math.sqrt(stats.impurity)
      println(
        f"${prefix}%-8s " +
        f"impurity = ${stats.impurity}% 6.4f " +
        f"predict = ${stats.predict}% 6.4f " +
        f"stdev = $stdev% 6.4f")
    } else {
      val split = node.split.get
      if (split.featureType == FeatureType.Continuous) {
        println(f"${prefix}%-8s " + 
          f"f:${featureMakerSeq(split.feature)}%-8s " +
          f"v:${split.threshold}% 6.4f" )
      } else {
        println(f"${prefix}%-8s " + 
          f"f:${featureMakerSeq(split.feature)}%-8s " +
          f"c:${split.categories}" )
      }
      println(f"         " + node.stats.get)
      p(prefix + "L", node.leftNode.get)
      p(prefix + "R", node.rightNode.get)
    }
  }
}

trait FeatureMaker extends Serializable {
  def categories: Int = 0
  def make(
    lgPrice: Series[DateTime, Double],
    mktLgPrice: Series[DateTime, Double]): Series[DateTime, Double]
}

class ReturnFeature(val d: Int) extends FeatureMaker {
  def make(lgPrice: Series[DateTime, Double]) = {
    (lgPrice - lgPrice.shift(d)).fillNA(_ => 0.0)
  }

  def make(
    lgPrice: Series[DateTime, Double],
    mktLgPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
    make(lgPrice)
  }
  override def toString(): String = s"ret$d"
}

class MAFeature(val d: Int) extends FeatureMaker {
  //override def categories: Int = 2
  def make(
    lgPrice: Series[DateTime, Double],
    mktLgPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
    val maPrice = SeriesRollingStats(lgPrice)
      .rollingMean(d)
      .reindex(lgPrice.index)
      .fillNA(_ => 0.0)
    (lgPrice - maPrice).mapValues(v => (if (v >= 0) 1.0 else 0.0))
  }
  override def toString(): String = s"ma$d"
}

class MAReturnFeature(val d: Int) extends FeatureMaker {
  val returnFeature = new ReturnFeature(1)
  def make(
    lgPrice: Series[DateTime, Double],
    mktLgPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
    val maPrice = SeriesRollingStats(lgPrice)
      .rollingMean(d)
      .reindex(lgPrice.index)
      .fillNA(_ => 0.0)
    returnFeature.make(maPrice)
  }
  override def toString(): String = s"ma${d}_ret"
}

class MktReturnFeature(val d: Int) extends FeatureMaker {
  val returnFeature = new ReturnFeature(d)
  def make(
    lgPrice: Series[DateTime, Double],
    mktLgPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
    returnFeature.make(mktLgPrice)
  }
  override def toString(): String = s"mkt_ret$d"
}



object SparkStock {
  val tickerList = Seq("SPY")
  /*
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
    "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
    "XRX", "YHOO", "AMAT", "QCOM", "TXN",
    "CRM", "INTU", "WDC", "SNDK")
  */
  //val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
  //  "HPQ", "INTC", "NTAP", "CSCO", "ORCL")

  //val tickerList = Seq("IBM", "MSFT")
  //val tickerList = Seq("IBM", "MSFT", "GOOG")
  //val tickerList = Seq("GOOG", "AAPL", "MSFT")//, "MSFT", "IBM")
    //"HPQ", "INTC", "NTAP", "CSCO", "ORCL")

  type EI = Int  // Evaluation Index
  /*
  def makeFeature(price: Series[DateTime, Double]): Array[Double] = {
    price.tail(5).values.contents
  }
  */


  def makeLabeledPoints(
    tickers: Seq[String],
    price: Frame[DateTime, String, Double],
    featureMakerSeq: Seq[FeatureMaker])
  : Seq[LabeledPoint] = {
    val points = ArrayBuffer[LabeledPoint]()
    //val tickers = price.colIx.toVec.contents
    val timeLength = price.rowIx.length
      
    val firstIdx = 30
    val lastIdx = timeLength - 1

    val mktLgPrice = price.firstCol("SPY").mapValues(math.log)

    tickers
    .filter(ticker => price.colIx.contains(ticker))
    .map { ticker => {
      val lgPrice = price.firstCol(ticker).mapValues(math.log)
      
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
          println(s"$ticker ${price.rowIx.raw(i)} ${fwdRet.raw(i)}")
          assert(false)
        }
        val p = new LabeledPoint(fwdRet.raw(i), Vectors.dense(feature))
        points += p
      })
      //ps 
    }}
    points.toSeq
  }


  def main(args: Array[String]) = {
    val evalDataParams = new EvaluationDataParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      untilIdx = 710,
      //untilIdx = 1200,
      trainingWindowSize = 600,
      evaluationInterval = 300,
      marketTicker = "SPY",
      tickerList = tickerList)
    
    val evaluator = BackTestingEvaluator()
    val dataPrep = evaluator.dataPreparatorClass.newInstance

    val localParamsSet = dataPrep
      .getParamsSetBase(evalDataParams)
      .zipWithIndex
      .map(_.swap)

    val localValidationParamsSet = localParamsSet.map(e => (e._1, e._2._2)) 
    val localTrainingParamsSet = localParamsSet.map(e => (e._1, e._2._1)) 

    val localValidationParams = localValidationParamsSet.head._2
    val localTrainingParams = localTrainingParamsSet.head._2

    val localValidationData = dataPrep
      .prepareValidationBase(localValidationParams)

    val localTrainingData = dataPrep
      .prepareTrainingBase(localTrainingParams)

    //var validationParamsMap: RDD[(EI, BaseValidationDataParams)] = 
    //  sc.parallelize(localValidationParamsSet)

    val conf = new SparkConf().setAppName(s"PredictionIO: Spark Stock")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val validationData = sc.parallelize(localValidationData)
    val trainingData = sc.parallelize(Seq(localTrainingData))

    /*
    validationData.collect.foreach{ e => {
      println(s"V: ${e._1} ${e._2}")
    }}
    */

    val featureMakerSeq = Seq(
      new ReturnFeature(5),
      new ReturnFeature(22),
      new MAFeature(5),
      new MAFeature(22),
      new MAReturnFeature(5),
      new MAReturnFeature(22),
      //new MktReturnFeature(1),
      //new MktReturnFeature(5),
      //new MktReturnFeature(22)
      new ReturnFeature(1)
    )

    val data = trainingData.flatMap(
      e => makeLabeledPoints(e.tickers, e.price, featureMakerSeq)
    )

    val maxDepth = 4
    val strategy = new Strategy(
      algo = Regression, 
      impurity = Variance, 
      maxDepth = maxDepth,
      categoricalFeaturesInfo = featureMakerSeq
        .zipWithIndex
        .filter(_._1.categories >= 1)
        .map(e => (e._2 -> e._1.categories))
        .toMap
    )
    val model = DecisionTree.train(data, strategy)

    println(model)

    println("Data size: " + data.count)

    Print.p(model)(featureMakerSeq)

    // Evaluate model on training examples and compute training error
    /*
    val valuesAndPreds = data.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val MSE = valuesAndPreds.map{ case(v, p) => math.pow((v - p), 2)}.mean()
    println("training Mean Squared Error = " + MSE) 
    */
    
  }
}

