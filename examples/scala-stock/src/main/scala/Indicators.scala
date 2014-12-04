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
 * Base class for indicators. All indicators should be defined as classes that extend
 * this base class. See RSIIndicator as an example. These indicators can then be
 * instantiated and passed into a StockStrategy class.
 */
@SerialVersionUID(100L)
abstract class BaseIndicator extends Serializable {
	def getTraining(logPrice: Series[DateTime, Double]): Series[DateTime, Double]
	def getOne(input: Series[DateTime, Double]): Double
	def minWindowSize(): Int
}

// make params to RSIIndicator a case class?

// Calculates RSI day averages
class RSIIndicator(onCloseWindowSize: Int = 14, period: Int) extends BaseIndicator {
	// RSI is always between 0 and 100

	private def getRet(logPrice: Series[DateTime, Double]) =
		(logPrice - logPrice.shift(period)).fillNA(_ => 0.0)

	def minWindowSize(): Int = onCloseWindowSize

	/*
	* @authors - Matt & Leta
	*/
	private def calcRS(logPrice: Series[DateTime, Double], period: Int): Series[DateTime, Double] = {
		//Positive and Negative Vecs
		val posSeries = logPrice.mapValues[Double]( (x:Double) => if (x > 0) x else 0)
		val negSeries = logPrice.mapValues[Double]( (x:Double) => if (x < 0) x else 0)
		//Get the sum of positive/negative Frame
		val avgPosSeries = posSeries.rolling[Double] (period, (f: Series[DateTime,Double]) => f.mean)
		val avgNegSeries = negSeries.rolling[Double] (period, (f: Series[DateTime,Double]) => f.mean)

		val rsSeries = avgPosSeries/avgNegSeries
		rsSeries
	}

	// Input: price data: i.e. from Nov1, 2012 until Nov1, 2014 
  	// Output: RSI from Nov1, 2012 until Nov1, 2014
	def getTraining(logPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
		val rsSeries = calcRS(getRet(logPrice), 14)
		val rsiSeries = rsSeries.mapValues[Double]( (x:Double) => 100 - (100/(1 + x)))

		// Fill in first 14 days offset with zero
		rsiSeries.reindex(logPrice.rowIx).fillNA(_  => 0.0)
	}

	// Input: price data: i.e. from Oct 15, 2014 until Nov 15, 2014.
  	// Output: RSI on Nov 15, 2014
	def getOne(logPrice: Series[DateTime, Double]): Double = {
		getTraining(logPrice).last
	}
}

// Indicator that calcuate differences of opening/closing prices
class ShiftsIndicator(onCloseWindowSize: Int = 14, period: Int) extends BaseIndicator {

	private def getRet(logPrice: Series[DateTime, Double], frameShift:Int = period) =
		(logPrice - logPrice.shift(frameShift)).fillNA(_ => 0.0)

	def minWindowSize(): Int = onCloseWindowSize

	def getTraining(logPrice: Series[DateTime, Double]): Series[DateTime, Double] = {
		getRet(logPrice)
	}

	def getOne(logPrice: Series[DateTime, Double]): Double = {
		getRet(logPrice).last
	}
}
