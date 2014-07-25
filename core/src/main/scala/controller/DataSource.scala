package io.prediction.controller

import io.prediction.core.BaseDataSource

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._

/** Base class of a local data source.
  *
  * A local data source runs locally within a single machine and return data
  * that can fit within a single machine.
  *
  * @tparam DSP Data source parameters class.
  * @tparam DP Data parameters data class.
  * @tparam TD Training data class.
  * @tparam Q Input query class.
  * @tparam A Actual value class.
  * @group Data Source
  */
abstract class LDataSource[
    DSP <: Params : ClassTag,
    DP,
    TD : ClassTag,
    Q,
    A]
  extends BaseDataSource[DSP, DP, RDD[TD], Q, A] {

  def readBase(sc: SparkContext): Seq[(DP, RDD[TD], RDD[(Q, A)])] = {
    read.map { case (dp, td, qaSeq) => {
      (dp, sc.parallelize(Array(td)), sc.parallelize(qaSeq))
    }}
  }

  /** Implement this method to return data from a data source. Returned data
    * can optionally include a sequence of query and actual value pairs for
    * evaluation purpose.
    */
  def read(): Seq[(DP, TD, Seq[(Q, A)])]
}

/** Base class of a local sliced data source.
  *
  * A local sliced data source runs locally within a single machine and return a
  * single slice of data that can fit within a single machine.
  *
  * @tparam DSP Data source parameters class.
  * @tparam DP Data parameters data class.
  * @tparam TD Training data class.
  * @tparam Q Input query class.
  * @tparam A Actual value class.
  * @group Data Source
  */
abstract class LSlicedDataSource[
    DSP <: Params : ClassTag,
    DP,
    TD : ClassTag,
    Q,
    A]
  extends LDataSource[DSP, DP, TD, Q, A] {

  def read(): Seq[(DP, TD, Seq[(Q, A)])] = {
    generateDataParams().map { dp =>
      val slicedData = read(dp)
      (dp, slicedData._1, slicedData._2)
    }
  }

  /** Implement this method to return a sequence of data parameters. Each of
    * these can be supplied to `read(dp)` to obtain a single slice of training
    * data.
    */
  def generateDataParams(): Seq[DP]

  /** Implement this method to return data from a data source with given data
    * parameters. Returned data slice can optionally include a sequence of query
    * and actual value pairs for evaluation purpose.
    *
    * @param dp Data parameters.
    */
  def read(dp: DP): (TD, Seq[(Q, A)])
}

/** Base class of a parallel data source.
  *
  * A parallel data source runs locally within a single machine, or in parallel
  * on a cluster, to return data that is distributed across a cluster.
  *
  * @tparam DSP Data source parameters class.
  * @tparam DP Data parameters data class.
  * @tparam TD Training data class.
  * @tparam Q Input query class.
  * @tparam A Actual value class.
  * @group Data Source
  */
abstract class PDataSource[DSP <: Params : ClassTag, DP, TD, Q, A]
  extends BaseDataSource[DSP, DP, TD, Q, A] {

  def readBase(sc: SparkContext): Seq[(DP, TD, RDD[(Q, A)])] = {
    read(sc).map { case (dp, td, qaRdd) => {
      // TODO(yipjustin). Maybe do a size check on td, to make sure the user
      // doesn't supply a huge TD to the driver program.
      (dp, td, qaRdd)
    }}
  }

  /** Implement this method to return data from a data source. Returned data
    * can optionally include a sequence of query and actual value pairs for
    * evaluation purpose.
    */
  def read(sc: SparkContext): Seq[(DP, TD, RDD[(Q, A)])]
}
