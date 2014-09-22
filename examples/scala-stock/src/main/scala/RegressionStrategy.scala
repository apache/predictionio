package io.prediction.examples.stock

import org.saddle._
import org.saddle.index.IndexTime

import scala.collection.immutable.HashMap
import breeze.linalg.DenseMatrix
import breeze.linalg.DenseVector

import com.github.nscala_time.time.Imports._

import scala.math

import nak.regress.LinearRegression

class RegressionStrategy
    extends StockStrategy[Map[String, DenseVector[Double]]] {

  private def getRet(logPrice: Frame[DateTime, String, Double], d: Int) =
    (logPrice - logPrice.shift(d)).mapVec[Double](_.fillNA(_ => 0.0))

  private def regress(
    ret1d: Series[DateTime, Double],
    ret1w: Series[DateTime, Double],
    ret1m: Series[DateTime, Double],
    retF1d: Series[DateTime, Double]) = {
    val array = (
      Seq(ret1d, ret1w, ret1m).map(_.toVec.contents).reduce(_ ++ _) ++
      Array.fill(ret1d.length)(1.0))
    val target = DenseVector[Double](retF1d.toVec.contents)
    val m = DenseMatrix.create[Double](ret1d.length, 4, array)
    val result = LinearRegression.regress(m, target)
    result
  }
  
  def createModel(dataView: DataView): Map[String, DenseVector[Double]] = {
    val price = dataView.priceFrame(windowSize = 30)
    val logPrice = price.mapValues(math.log)

    val ret1d = getRet(logPrice, 1)
    val ret1w = getRet(logPrice, 5)
    val ret1m = getRet(logPrice, 22)
    val retF1d = getRet(logPrice, -1)

    val timeIndex = price.rowIx
    val firstIdx = 25
    val lastIdx = timeIndex.length

    val tickers = price.colIx.toVec.contents

    val tickerModelMap = tickers.map(ticker => {
      val model = regress(
        ret1d.firstCol(ticker).slice(firstIdx, lastIdx),
        ret1w.firstCol(ticker).slice(firstIdx, lastIdx),
        ret1m.firstCol(ticker).slice(firstIdx, lastIdx),
        retF1d.firstCol(ticker).slice(firstIdx, lastIdx))
      (ticker, model)
    }).toMap

    tickerModelMap
  }

  def onClose(model: Map[String, DenseVector[Double]], query: Query)
  : Prediction = {
    Prediction(data = HashMap[String, Double]())
  }
}
