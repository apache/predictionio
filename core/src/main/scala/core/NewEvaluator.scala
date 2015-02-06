package n.io.prediction.core

import io.prediction.core.AbstractDoer
import scala.reflect._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

abstract class BaseEvaluator[-EI, Q, P, A, ER <: AnyRef]
  extends AbstractDoer {

  def evaluateBase(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])]): ER
}

