package io.prediction.controller.java

import io.prediction.core.BasePreparator
import io.prediction.controller.Params
import io.prediction.controller.EmptyParams

import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._

import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.reflect._

/**
 * Base class of a local preparator.
 *
 * A local preparator runs locally within a single machine and produces prepared
 * data that can fit within a single machine.
 *
 * @param <PP> Preparator Parameters
 * @param <TD> Training Data
 * @param <PD> Prepared Data
 */
abstract class LJavaPreparator[PP <: Params, TD, PD]
  extends BasePreparator[PP, RDD[TD], RDD[PD]]() (
    JavaUtils.fakeClassTag[PP]) {

  def prepareBase(sc: SparkContext, td: RDD[TD]): RDD[PD] = {
    implicit val fakeTdTag: ClassTag[PD] = JavaUtils.fakeClassTag[PD]
    td.map(prepare)
  }

  /**
   * Implement this method to produce prepared data that is ready for model
   * training.
   */
  def prepare(td: TD): PD
}

/**
 * A helper concrete implementation of {@link LJavaPreparator} that pass
 * training data through without any special preparation.
 *
 * @param <TD> Training Data
 */
class LJavaIdentityPreparator[TD] extends LJavaPreparator[EmptyParams, TD, TD] {
  override def prepare(td: TD): TD = td
}

/**
 * A helper concrete implementation of {@link LJavaPreparator} that pass
 * training data through without any special preparation.
 */
object LJavaIdentityPreparator {
  /** Produces an instance of {@link LJavaIdentityPreparator}. */
  def apply[TD](ds: Class[_ <: LJavaDataSource[_, _, TD, _, _]]) =
    classOf[LJavaIdentityPreparator[TD]]
}
