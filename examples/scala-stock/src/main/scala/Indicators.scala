package io.prediction.examples.stock

import org.saddle._
import org.saddle.index.IndexTime

import scala.collection.immutable.HashMap
import breeze.linalg.DenseMatrix
import breeze.linalg.DenseVector

import com.github.nscala_time.time.Imports._

import scala.math

import math._

import nak.regress.LinearRegression

/*
 * Base class for indicators. All indicators should be defined as classes that 
 * extend this base class. See RSIIndicator as an example. These indicators can 
 * then be instantiated and passed into a StockStrategy class. Refer to 
 * tutorial for further explanation (found in the README.md file).
 */
@SerialVersionUID(100L)
abstract class BaseIndicator extends Serializable {
  def getTraining(logPrice: Series[DateTime, Double]): Series[DateTime, Double]
  def getOne(input: Series[DateTime, Double]): Double
  def getMinWindowSize(): Int
}

/* Calculates RSI day averages
 * Input: period - differences in closing cost is calculated over
 */
class RSIIndicator(period: Int, RsiPeriod: Int = 14) extends BaseIndicator {

	private def getRet(dailyReturn: Series[DateTime, Double]) =
		(dailyReturn - dailyReturn.shift(period)).fillNA(_ => 0.0)

	def getMinWindowSize(): Int = RsiPeriod + 1

	private def calcRS(logPrice: Series[DateTime, Double]): 
    Series[DateTime, Double] = {
		//Positive and Negative Vecs
		val posSeries = logPrice.mapValues[Double]((x:Double) => if (x > 0) x else 0)
		val negSeries = logPrice.mapValues[Double]((x:Double) => if (x < 0) x else 0)
		//Get the sum of positive/negative Frame
		val avgPosSeries = posSeries.rolling[Double] 
      (RsiPeriod, (f: Series[DateTime,Double]) => f.mean)
		val avgNegSeries = negSeries.rolling[Double] 
      (RsiPeriod, (f: Series[DateTime,Double]) => f.mean)

		val rsSeries = avgPosSeries / avgNegSeries
		rsSeries
	}

	// Computes RSI of price data over the defined training window time frame
	def getTraining(logPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
		val rsSeries = calcRS(getRet(logPrice))
		val rsiSeries = rsSeries.mapValues[Double]( 
        (x:Double) => 100 - ( 100 / (1 + x)))

		// Fill in first 14 days offset with 50 to maintain results
		rsiSeries.reindex(logPrice.rowIx).fillNA(_  => 50.0)
	}

  	// Computes the RSI for the most recent time frame, returns single double
	def getOne(logPrice: Series[DateTime, Double]): Double = {
		getTraining(logPrice).last
	}
}

// Indicator that calcuates differences of closing prices
class ShiftsIndicator(period: Int) extends BaseIndicator {

  private def getRet(logPrice: Series[DateTime, Double], frameShift: Int = period) =
		(logPrice - logPrice.shift(frameShift)).fillNA(_ => 0.0)

	def getMinWindowSize(): Int = period + 1

	def getTraining(logPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
		getRet(logPrice)
	}

	def getOne(logPrice: Series[DateTime, Double]): Double = {
		getRet(logPrice).last
	}
}
