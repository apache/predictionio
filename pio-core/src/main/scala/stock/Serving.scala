package io.prediction.stock

import io.prediction.BaseServer
import scala.math
import org.saddle._
import org.saddle.index.IndexTime

import scala.collection.mutable.{ Map => MMap }
import com.github.nscala_time.time.Imports._

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.stats.{ mean, meanAndVariance }
import nak.regress.LinearRegression
import scala.collection.mutable.ArrayBuffer

class Server extends BaseServer[Model, Feature, Target] {
  private def predictOne(
    coef: DenseVector[Double],
    price: Series[DateTime, Double]): Double = {
    val shifts = Seq(0, 1, 5, 22)
    val sp = shifts
      .map(s => (s, math.log(price.raw(price.length - s - 1))))
      .toMap

    val vec = DenseVector[Double](
      sp(0) - sp(1),
      sp(0) - sp(5),
      sp(0) - sp(22),
      1)
   
    val p = coef.dot(vec)
    return p 
  }

  def predict(model: Model, feature: Feature): Target = {
    val price: Frame[DateTime, String, Double] = feature.data
    val modelData = model.data
    val prediction = price.colIx.toVec.contents
    // If model doesn't have the data, skip
    .filter{ ticker => model.data.contains(ticker) }  
    .map{ ticker => {
      val p = predictOne(
        modelData(ticker), 
        price.firstCol(ticker))
      (ticker, p)
    }}.toMap
    new Target(data = prediction)
  }
}
