package io.prediction.controller

import io.prediction.workflow.SharedSparkContext

import org.scalatest.FunSuite
import org.scalatest.Inside
import org.scalatest.Matchers._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object EvaluationSuite {
  import io.prediction.controller.TestEvaluator._

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
  import io.prediction.controller.EvaluationSuite._

  test("Evaluation makes MetricEvaluator") {
    // MetricEvaluator is typed [EvalInfo, Query, Prediction, Actual, Int],
    // however this information is erased on JVM. scalatest doc recommends to
    // use wildcards.
    Evaluation0.evaluator shouldBe a [MetricEvaluator[_, _, _, _, _]]
  }

  test("Load from class path") {
    val r = io.prediction.workflow.WorkflowUtils.getEvaluation(
      "io.prediction.controller.EvaluationSuite.Evaluation0",
      getClass.getClassLoader)

    r._2 shouldBe EvaluationSuite.Evaluation0
  }

}
