/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.predictionio.controller

import org.apache.predictionio.workflow.SharedSparkContext

import org.scalatest.FunSuite
import org.scalatest.Inside
import org.scalatest.Matchers._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object EvaluationSuite {
  import org.apache.predictionio.controller.TestEvaluator._

  class Metric0 extends Metric[EvalInfo, Query, Prediction, Actual, Int] {
    def calculate(
      sc: SparkContext,
      evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])]): Int = 1
  }

  object Evaluation0 extends Evaluation {
    engineMetric = (new FakeEngine(1, 1, 1), new Metric0())
  }
}


class EvaluationSuite
extends FunSuite with Inside with SharedSparkContext {
  import org.apache.predictionio.controller.EvaluationSuite._

  test("Evaluation makes MetricEvaluator") {
    // MetricEvaluator is typed [EvalInfo, Query, Prediction, Actual, Int],
    // however this information is erased on JVM. scalatest doc recommends to
    // use wildcards.
    Evaluation0.evaluator shouldBe a [MetricEvaluator[_, _, _, _, _]]
  }

  test("Load from class path") {
    val r = org.apache.predictionio.workflow.WorkflowUtils.getEvaluation(
      "org.apache.predictionio.controller.EvaluationSuite.Evaluation0",
      getClass.getClassLoader)

    r._2 shouldBe EvaluationSuite.Evaluation0
  }

}
