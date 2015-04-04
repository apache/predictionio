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

import grizzled.slf4j.Logger
import io.prediction.workflow.PersistentModelManifest
import io.prediction.workflow.SharedSparkContext
import io.prediction.workflow.StopAfterPrepareInterruption
import io.prediction.workflow.StopAfterReadInterruption
import org.apache.spark.rdd.RDD
import org.scalatest.Inspectors._
import org.scalatest.Matchers._
import org.scalatest.FunSuite
import org.scalatest.Inside

import scala.util.Random

object MetricEvaluatorSuite {
  case class Metric0() extends SumMetric[EmptyParams, Int, Int, Int, Int] {
    def calculate(q: Int, p: Int, a: Int): Int = q
  }

  object Evaluation0 extends Evaluation {}
}

class MetricEvaluatorDevSuite
extends FunSuite with SharedSparkContext {
  import io.prediction.controller.MetricEvaluatorSuite._

  test("a") {
    val metricEvaluator = MetricEvaluator(
      Metric0(),
      Seq(Metric0(), Metric0())
    )
 
    val engineEvalDataSet = Seq(
      (EngineParams(), Seq(
        (EmptyParams(), sc.parallelize(Seq((1,0,0), (2,0,0)))))),
      (EngineParams(), Seq(
        (EmptyParams(), sc.parallelize(Seq((1,0,0), (2,0,0)))))))

    val r = metricEvaluator.evaluateBase(
      sc,
      Evaluation0,
      engineEvalDataSet,
      WorkflowParams())

  }

}

