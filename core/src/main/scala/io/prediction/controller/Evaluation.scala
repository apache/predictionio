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

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEvaluatorResult
import io.prediction.core.BaseEngine
// import io.prediction.workflow.EngineWorkflow

import scala.language.implicitConversions

/** Defines an evaluation that contains an engine and a metric.
  *
  * Implementations of this trait can be supplied to "pio eval" as the first
  * argument.
  *
  * @group Evaluation
  */
trait Evaluation extends Deployment {
  protected [this] var _evaluatorSet: Boolean = false
  protected [this] var _evaluator: BaseEvaluator[_, _, _, _, _ <: BaseEvaluatorResult] = _

  private [prediction] 
  def evaluator: BaseEvaluator[_, _, _, _, _ <: BaseEvaluatorResult] = {
    assert(_evaluatorSet, "Evaluator not set")
    _evaluator
  }
  
  def engineEvaluator
  : (BaseEngine[_, _, _, _], BaseEvaluator[_, _, _, _, _]) = {
    assert(_evaluatorSet, "Evaluator not set")
    (engine, _evaluator)
  }

  /** Sets both the [[Engine]] and [[Evaluator]] for this [[Evaluation]]. */
  def engineEvaluator_=[EI, Q, P, A, R <: BaseEvaluatorResult](
    engineEvaluator: (
      BaseEngine[EI, Q, P, A], 
      BaseEvaluator[EI, Q, P, A, R])) {
    assert(!_evaluatorSet, "Evaluator can be set at most once")
    engine = engineEvaluator._1
    _evaluator = engineEvaluator._2
    _evaluatorSet = true
  }

  /** Returns both the [[Engine]] and [[Metric]] contained in this
    * [[Evaluation]].
    */
  def engineMetric: (BaseEngine[_, _, _, _], Metric[_, _, _, _, _]) = {
    throw new NotImplementedError("This method is to keep the compiler happy")
  }

  /** Sets both the [[Engine]], [[Metric]] for this [Evaluation]] */
  def engineMetric_=[EI, Q, P, A](
    engineMetric: (BaseEngine[EI, Q, P, A], Metric[EI, Q, P, A, _])) {
    engineEvaluator = (
      engineMetric._1, 
      MetricEvaluator(
        metric = engineMetric._2,
        otherMetrics = Seq[Metric[EI, Q, P, A, _]](),
        outputPath = "best.json"))
  }

  /** Returns both the [[Engine]] and [[Metric]] contained in this
    * [[Evaluation]].
    *
    */
  private [prediction] 
  def engineMetrics: (BaseEngine[_, _, _, _], Metric[_, _, _, _, _]) = {
    throw new NotImplementedError("This method is to keep the compiler happy")
  }

  /** Sets the [[Engine]], [[Metric]], and Seq([[Metric]])) 
    * for this [[Evaluation]]. */
  def engineMetrics_=[EI, Q, P, A](
    engineMetrics: (
      BaseEngine[EI, Q, P, A], 
      Metric[EI, Q, P, A, _], 
      Seq[Metric[EI, Q, P, A, _]])) {
    engineEvaluator = (
      engineMetrics._1,
      MetricEvaluator(engineMetrics._2, engineMetrics._3))
  }
}
