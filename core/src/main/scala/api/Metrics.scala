package io.prediction.api

import io.prediction.core.BaseMetrics
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class Metrics[MP <: Params : ClassTag,
    DP, Q, P, A, MU, MR, MMR <: AnyRef]
  extends BaseMetrics[MP, DP, Q, P, A, MU, MR, MMR] {

  def computeUnitBase(input: (Q, P, A)): MU = {
    computeUnit(input._1, input._2, input._3) 
  }

  def computeUnit(query: Q, prediction: P, actual: A): MU

  def computeSetBase(dataParams: DP, metricUnits: Seq[MU]): MR = {
    computeSet(dataParams, metricUnits)
  }

  def computeSet(dataParams: DP, metricUnits: Seq[MU]): MR

  def computeMultipleSetsBase(input: Seq[(DP, MR)]): MMR = {
    computeMultipleSets(input)
  }
  
  def computeMultipleSets(input: Seq[(DP, MR)]): MMR
}

/****** Helper Functions ******/

// DP is AnyRef. This support any kind of DataParam.
class MeanSquareError[Q] extends Metrics[EmptyParams, AnyRef, 
    Q, Double, Double, (Double, Double), String, String] {
  def computeUnit(q: Q, p: Double, a: Double): (Double, Double) = (p, a)

  def computeSet(ep: AnyRef, data: Seq[(Double, Double)]): String = {
    val units = data.map(e => math.pow(e._1 - e._2, 2))
    val mse = units.sum / units.length
    f"Set: $ep Size: ${data.length} MSE: ${mse}%8.6f"
  }

  def computeMultipleSets(input: Seq[(AnyRef, String)]): String = {
    input.map(_._2).mkString("\n")
  }
}

object MeanSquareError {
  import _root_.io.prediction.core.BaseServing
  def apply[Q](serving: Class[_ <:  BaseServing[_, Q, _]]) = {
    classOf[MeanSquareError[Q]]
  }
}
