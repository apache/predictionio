package n.io.prediction.controller

import n.io.prediction.core.BaseDataSource

import io.prediction.core.AbstractDoer
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._

/** Base class of a parallel data source.
  *
  * A parallel data source runs locally within a single machine, or in parallel
  * on a cluster, to return data that is distributed across a cluster.
  *
  * @tparam TD Training data class.
  * @tparam EI Evalution Info class.
  * @tparam Q Input query class.
  * @tparam A Actual value class.
  * @group Data Source
  */

abstract class PDataSource[TD, EI, Q, A]
  extends BaseDataSource[TD, EI, Q, A] {

  def readTrainBase(sc: SparkContext): TD = readTrain(sc)

  def readTrain(sc: SparkContext): TD
  
  def readEvalBase(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])] = readEval(sc)

  def readEval(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])] = {
    Seq[(TD, EI, RDD[(Q, A)])]()
  }
}


/** Base class of a local data source.
  *
  * A local data source runs locally within a single machine and return data
  * that can fit within a single machine.
  *
  * @tparam TD Training data class.
  * @tparam EI Evalution Info class.
  * @tparam Q Input query class.
  * @tparam A Actual value class.
  * @group Data Source
  */
abstract class LDataSource[
    TD : ClassTag,
    EI : ClassTag,
    Q,
    A]
  extends BaseDataSource[RDD[TD], EI, Q, A] {
  
  def readTrainBase(sc: SparkContext): RDD[TD] = {
    sc.parallelize(Seq(None)).map(_ => readTrain())
  }

  def readTrain(): TD

  //def readEvalBase(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])]
  def readEvalBase(sc: SparkContext): Seq[(RDD[TD], EI, RDD[(Q, A)])] = readEval(sc)

  // FIXME
  def readEval(sc: SparkContext): Seq[(RDD[TD], EI, RDD[(Q, A)])] = {
    Seq[(RDD[TD], EI, RDD[(Q, A)])]()
  }
}



