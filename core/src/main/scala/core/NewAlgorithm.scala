package n.io.prediction.core

import io.prediction.core.AbstractDoer
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class BaseAlgorithm[PD, M, Q, P]
  extends AbstractDoer {
  def trainBase(sc: SparkContext, pd: PD): M

  def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Q)])
  : RDD[(Long, P)]

  /*
  def predictBase(bm: Any, q: Q): P
  */
}


