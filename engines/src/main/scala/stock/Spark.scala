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
import org.apache.spark.mllib.tree.impurity.Gini
import org.apache.spark.mllib.tree.impurity.Variance


import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._

object Print {
  import org.apache.spark.mllib.tree.model._

  def p(model: DecisionTreeModel): Unit = {
    println("Print model")
    p("", model.topNode)
  }

  def p(prefix: String, node: Node): Unit = {
    if (node.isLeaf) {
      val stats = node.stats.get
      val stdev = math.sqrt(stats.impurity)
      println(
        f"${prefix}%8s " +
        f"impurity = ${stats.impurity}% 6.4f " +
        f"predict = ${stats.predict}% 6.4f " +
        f"stdev = $stdev% 6.4f")
    } else {
      println(f"${prefix}%8s " + node.split.get)
      println(f"${prefix}%8s " + node.stats.get)
      p(prefix + "L", node.leftNode.get)
      p(prefix + "R", node.rightNode.get)
    }
  }
}


object SparkStock {
  val tickerList = Seq("GOOG", "AAPL", "AMZN", "MSFT", "IBM",
    "HPQ", "INTC", "NTAP", "CSCO", "ORCL",
    "XRX", "YHOO", "AMAT", "QCOM", "TXN",
    "CRM", "INTU", "WDC", "SNDK")

  type EI = Int  // Evaluation Index
  private def getRet(logPrice: Frame[DateTime, String, Double], d: Int) =
    (logPrice - logPrice.shift(d)).mapVec[Double](_.fillNA(_ => 0.0))
  
  def makeFeature(price: Series[DateTime, Double]): Array[Double] = {
    price.tail(5).values.contents
    /*
    val logPrice = price.mapValues(math.log)
    val d = 1
    val logRet = (logPrice - logPrice.shift(d)).fillNA(_ => 0.0)
    logRet.tail(5).values.contents
    */
  }

  def main(args: Array[String]) = {
    val evalDataParams = new EvaluationDataParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      //untilIdx = 710,
      untilIdx = 1200,
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

    val localValidationParams = localValidationParamsSet.head._2


    val localValidationData = dataPrep
      .prepareValidationBase(localValidationParams)

    //var validationParamsMap: RDD[(EI, BaseValidationDataParams)] = 
    //  sc.parallelize(localValidationParamsSet)

    val conf = new SparkConf().setAppName(s"PredictionIO: Spark Stock")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val validationData = sc.parallelize(localValidationData)

    /*
    validationData.collect.foreach{ e => {
      println(s"V: ${e._1} ${e._2}")
    }}
    */

    val data = validationData.flatMap{ case (f, a) => {
      val price = f.data
      val logPrice = price.mapValues(math.log)
      val logRet = getRet(logPrice, 1)

      val tickers = f.data.colIx.toVec.contents
      tickers.map{ t => {
        val feature = logRet.firstCol(t).tail(5).values.contents
        val actual = a.data(t)
        LabeledPoint(actual, Vectors.dense(feature))
      }}
    }}

    val maxDepth = 5
    val model = DecisionTree.train(data, Regression, Variance, maxDepth)

    println(model)

    Print.p(model)


    // Evaluate model on training examples and compute training error
    val valuesAndPreds = data.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val MSE = valuesAndPreds.map{ case(v, p) => math.pow((v - p), 2)}.mean()
    println("training Mean Squared Error = " + MSE) 

  }
}

