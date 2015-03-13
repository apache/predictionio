/** Copyright 2015 TappingStone, Inc.
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

package io.prediction.controller

import io.prediction.core.BaseDataSource

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
  * @tparam EI Evaluation Info class.
  * @tparam Q Input query class.
  * @tparam A Actual value class.
  * @group Data Source
  */

abstract class PDataSource[TD, EI, Q, A]
  extends BaseDataSource[TD, EI, Q, A] {

  def readTrainingBase(sc: SparkContext): TD = readTraining(sc)

  /** Implement this method to only return training data from a data source.
    */
  def readTraining(sc: SparkContext): TD
  
  def readEvalBase(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])] = readEval(sc)

  /** Implement this method to return data from a data source. Returned data
    * can optionally include a sequence of query and actual value pairs for
    * evaluation purpose.
    */
  def readEval(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])] = {
    Seq[(TD, EI, RDD[(Q, A)])]()
  }

  /* Deprecated, use readEval. */
  def read(sc: SparkContext): Seq[(TD, EI, RDD[(Q, A)])] = readEval(sc)
}

/** Base class of a local data source.
  *
  * A local data source runs locally within a single machine and return data
  * that can fit within a single machine.
  *
  * @tparam TD Training data class.
  * @tparam EI Evaluation Info class.
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
  
  def readTrainingBase(sc: SparkContext): RDD[TD] = {
    sc.parallelize(Seq(None)).map(_ => readTraining())
  }

  /** Implement this method to only return training data from a data source.
    */
  def readTraining(): TD
  
  /** Implement this method to return data from a data source. Returned data
    * can optionally include a sequence of query and actual value pairs for
    * evaluation purpose.
    */
  def readEvalBase(sc: SparkContext): Seq[(RDD[TD], EI, RDD[(Q, A)])] = {
    val localEvalData: Seq[(TD, EI, Seq[(Q, A)])] = readEval()

    localEvalData.map { case (td, ei, qaSeq) => {
      val tdRDD = sc.parallelize(Seq(None)).map(_ => td)
      val qaRDD = sc.parallelize(qaSeq)
      (tdRDD, ei, qaRDD)
    }}
  }

  def readEval(): Seq[(TD, EI, Seq[(Q, A)])] = Seq[(TD, EI, Seq[(Q, A)])]()
  
  /* Deprecated, use readEval. */
  def read(): Seq[(TD, EI, Seq[(Q, A)])] = readEval()
}
