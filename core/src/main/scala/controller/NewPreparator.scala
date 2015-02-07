/*
package n.io.prediction.controller

import n.io.prediction.core.BaseDataSource
import n.io.prediction.core.BasePreparator
import io.prediction.core.AbstractDoer

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._
*/

/** Base class of a parallel preparator.
  *
  * A parallel preparator can be run in parallel on a cluster and produces a
  * prepared data that is distributed across a cluster.
  *
  * @tparam TD Training data class.
  * @tparam PD Prepared data class.
  * @group Preparator
  */

/*
abstract class PPreparator[TD, PD]
  extends BasePreparator[TD, PD] {

  def prepareBase(sc: SparkContext, td: TD): PD = {
    prepare(sc, td)
  }

  def prepare(sc: SparkContext, trainingData: TD): PD
}
*/

/** Base class of a local preparator.
  *
  * A local preparator runs locally within a single machine and produces
  * prepared data that can fit within a single machine.
  *
  * @tparam TD Training data class.
  * @tparam PD Prepared data class.
  * @group Preparator
  */
/*
abstract class LPreparator[TD, PD : ClassTag]
  extends BasePreparator[RDD[TD], RDD[PD]] {

  def prepareBase(sc: SparkContext, rddTd: RDD[TD]): RDD[PD] = {
    rddTd.map(prepare)
  }

  def prepare(trainingData: TD): PD
}
*/
