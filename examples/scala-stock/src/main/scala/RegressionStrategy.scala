package io.prediction.examples.stock

import io.prediction.controller.Params

import org.saddle._
import org.saddle.index.IndexTime

import scala.collection.immutable.HashMap
import breeze.linalg.DenseMatrix
import breeze.linalg.DenseVector

import com.github.nscala_time.time.Imports._

import scala.math

import nak.regress.LinearRegression

case class RegressionStrategyParams (
  indicators: Seq[(String, BaseIndicator)],
  maxTrainingWindowSize: Int
) extends Params

class RegressionStrategy (params: RegressionStrategyParams) extends StockStrategy[Map[String, DenseVector[Double]]] {
  val shifts = Seq(0, 1, 5, 22) // days used in regression model

  private def getRet(logPrice: Frame[DateTime, String, Double], d: Int) =
    (logPrice - logPrice.shift(d)).mapVec[Double](_.fillNA(_ => 0.0))

  /* Regress on specific ticker */
  private def regress(
    calculatedData: Seq[Series[DateTime, Double]],
    retF1d: Series[DateTime, Double]) = {
    val array = (
      // comment this line
      calculatedData.map(_.toVec.contents).reduce(_++_) ++
      Array.fill(retF1d.length)(1.0)).toArray[Double]
    val target = DenseVector[Double](retF1d.toVec.contents)
    val m = DenseMatrix.create[Double](retF1d.length, calculatedData.length + 1, array)
    val result = LinearRegression.regress(m, target)
    result
  }

  /* Train each indicator */
  private def trainAllIndicators(logPrice: Series[DateTime, Double]): Seq[Series[DateTime, Double]] = {
    var retSeq = Seq[Series[DateTime, Double]]()
    var x = 0
    for (x <- 0 to params.indicators.length - 1) {
      retSeq = retSeq ++ Seq(params.indicators(x)._2.getTraining(logPrice))
    }
    retSeq
  }

  // Get max period from series of indicators - firstIdx = getMaxPeriod + 3
  // private def getMaxPeriod()

  /* Apply regression algorithm on complete dataset to create a model */
  def createModel(dataView: DataView): Map[String, DenseVector[Double]] = {
    // price: row is time, col is ticker, values are prices
    val price = dataView.priceFrame(params.maxTrainingWindowSize)
    val logPrice = price.mapValues(math.log)
    val active = dataView.activeFrame(params.maxTrainingWindowSize) // tickers active within trainingWindow

    // value used to query prediction results
    val retF1d = getRet(logPrice, -1)

    val timeIndex = price.rowIx
    val firstIdx = shifts.max + 3   // only data past offset of 22 matters
    val lastIdx = timeIndex.length

    // Get array of ticker strings
    val tickers = price.colIx.toVec.contents

    // For each active ticker, pass in trained series into regress
    val tickerModelMap = tickers
    .filter(ticker => (active.firstCol(ticker).findOne(_ == false) == -1))
    .map(ticker => {
      val model = regress(
        // Only pass in valid data
        trainAllIndicators(price.firstCol(ticker)).map(_.slice(firstIdx, lastIdx)),
        retF1d.firstCol(ticker).slice(firstIdx, lastIdx))
      (ticker, model)
    }).toMap

    // tickers mapped to model
    tickerModelMap
  }

  private def predictOne(
    coef: DenseVector[Double],
    ticker: String,
    dataView: DataView): Double = {

    System.out.print("PredictOne: coefficient: ")
    var y = 0
    for (y <- 0 to coef.length - 1) {
      System.out.print( coef(y) )
    }
    System.out.println()

    var densVecArray = Array[Double]();
    var x = 0
    for (x <- 0 to params.indicators.length - 1) {
      // get most recent frame
      val price = dataView.priceFrame(params.indicators(x)._2.minWindowSize())
      val logPrice = price.mapValues(math.log)
      // comment this line
      densVecArray = densVecArray ++ Array[Double](params.indicators(x)._2.getOne(logPrice.firstCol(ticker)))
    }

    densVecArray = densVecArray ++ Array[Double](1)
    val vec = DenseVector[Double](densVecArray)
  
    val p = coef.dot(vec)
    // System.out.println("PredictOne: Dot product ressult: " )
    return p
  }

  /* Returns a mapping of tickers to predictions */
  def onClose(model: Map[String, DenseVector[Double]], query: Query)
  : Prediction = {
    val dataView = query.dataView

    val prediction = query.tickers
      .filter(ticker => model.contains(ticker))
      .map { ticker => {
        val p = predictOne(
          model(ticker),
          ticker,
          dataView)
        (ticker, p)
      }}

    Prediction(HashMap[String, Double](prediction:_*))
  }
}
