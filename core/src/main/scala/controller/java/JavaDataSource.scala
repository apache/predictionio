/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.controller.java

import io.prediction.core.BaseDataSource
import io.prediction.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.rdd.RDD

import java.util.{ List => JList }
import java.util.{ ArrayList => JArrayList }
import java.util.{ Collections => JCollections }
import java.lang.{ Iterable => JIterable }

import scala.collection.JavaConversions._
import scala.reflect._

/**
 * Base class of a local data source.
 *
 * A local data source runs locally within a single machine and return data that
 * can fit within a single machine.
 *
 * @param <TD> Training Data
 * @param <EI> Evaluation Info
 * @param <Q> Input Query
 * @param <A> Actual Value
 */
abstract class LJavaDataSource[TD, EI, Q, A]
  extends BaseDataSource[RDD[TD], EI, Q, A] {
  def readBase(sc: SparkContext): Seq[(RDD[TD], EI, RDD[(Q, A)])] = {
    implicit val fakeTdTag: ClassTag[TD] = JavaUtils.fakeClassTag[TD]
    val datasets = sc.parallelize(Array(None)).flatMap(_ => read().toSeq).zipWithIndex
    datasets.cache
    val eis: Map[Long, EI] = datasets.map(t => t._2 -> t._1._2).collect.toMap
    eis.map { t =>
      val dataset = datasets.filter(_._2 == t._1).map(_._1)
      val ei = t._2
      val td = dataset.map(_._1)
      val qa = dataset.map(_._3.toSeq).flatMap(identity)
      (td, ei, qa)
    }.toSeq
  }

  /** Implement this method to only return training data from a data source.
    */
  def readTraining(): TD = null.asInstanceOf[TD]

  /** Implement this method to return one set of test data (
    * an Iterable of query and actual value pairs) from a data source.
    * Should also implement readTraining to return correponding training data.
    */
  def readTest(): Tuple2[EI, JIterable[Tuple2[Q, A]]] =
    (null.asInstanceOf[EI], JCollections.emptyList())

  /** Implement this method to return one or more sets of training data
    * and test data (an Iterable of query and actual value pairs) from a
    * data source.
    */
  def read(): JIterable[Tuple3[TD, EI, JIterable[Tuple2[Q, A]]]] = {
    val (ei, qa) = readTest()
    List((readTraining(), ei, qa))
  }

}

/** Base class of a parallel data source.
  *
  * A parallel data source runs locally within a single machine, or in parallel
  * on a cluster, to return data that is distributed across a cluster.
  *
  * @param <TD> Training data class.
  * @param <EI> Evaluation info class.
  * @param <Q> Input query class.
  * @param <A> Actual value class.
  */
abstract class PJavaDataSource[TD, EI, Q, A]
  extends BaseDataSource[TD, EI, Q, A] {
  def readBase(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])] = {
    implicit val fakeTdTag: ClassTag[TD] = JavaUtils.fakeClassTag[TD]
    read(new JavaSparkContext(sc)).toSeq.map { case (td, ei, qaRdd) => {
      // TODO(yipjustin). Maybe do a size check on td, to make sure the user
      // doesn't supply a huge TD to the driver program.
      (td, ei, qaRdd.rdd)
    }}
  }

  /** Implement this method to return data from a data source. Returned data
    * can optionally include a sequence of query and actual value pairs for
    * evaluation purpose.
    */
  def read(jsc: JavaSparkContext): JIterable[Tuple3[TD, EI, JavaPairRDD[Q, A]]]
}
