package n.io.prediction.core

import io.prediction.core.AbstractDoer
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class BaseDataSource[TD, EI, Q, A]
  extends AbstractDoer {
  def readTrainBase(sc: SparkContext): TD

  def readEvalBase(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])]
}

