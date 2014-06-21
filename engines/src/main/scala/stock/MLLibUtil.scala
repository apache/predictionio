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

import scala.collection.mutable.{ Map => MMap }


trait FeatureMaker extends Serializable {
  def categories: Int = 0
  def make(
    lgPrice: Series[DateTime, Double],
    mktLgPrice: Series[DateTime, Double]): Series[DateTime, Double]
}

class ReturnFeature(val d: Int) extends FeatureMaker {
  def make(
    lgPrice: Series[DateTime, Double]) = {
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


object DecisionTreePrinter {
  import org.apache.spark.mllib.tree.model._
  import org.apache.spark.mllib.tree.configuration.FeatureType

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

