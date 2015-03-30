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
package io.prediction.e2.evaluation

import scala.reflect.ClassTag
import org.apache.spark.rdd.RDD

object CommonHelperFunctions {

  /**
   * Split a data set into evalK folds for crossvalidation. Apply to data sets supplied to evaluation.
   *
   * @tparam D Data point class.
   * @tparam TD Training data class.
   * @tparam EI Evaluation Info class.
   * @tparam Q Input query class.
   * @tparam A Actual value class.
   */

  def splitData[D: ClassTag, TD, EI, Q, A](

     evalK: Int,
     dataset: RDD[D],
     evaluatorInfo: EI,
     trainingDataCreator: RDD[D] => TD,
     queryCreator: D => Q,
     actualCreator: D => A): Seq[(TD, EI, RDD[(Q, A)])] = {

    val indexedPoints = dataset.zipWithIndex

    (0 until evalK).map { idx =>
      val trainingPoints = indexedPoints.flatMap { case (pt, i) if  i % evalK != idx => Some(pt) case _ => None}
      val testingPoints = indexedPoints.flatMap { case (pt, i) if  i % evalK == idx => Some(pt) case _ => None}

      (
        trainingDataCreator(trainingPoints),
        evaluatorInfo,
        testingPoints.map { d => (queryCreator(d), actualCreator(d)) }
      )
    }
  }
}
