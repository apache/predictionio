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

import org.apache.predictionio.core._
import org.apache.predictionio.workflow.WorkflowParams

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object TestEvaluator {
  case class EvalInfo(id: Int, ex: Int)
  case class Query(id: Int, ex: Int, qx: Int)
  case class Prediction(id: Int, ex: Int, qx: Int)
  case class Actual(id: Int, ex: Int, qx: Int)

  class FakeEngine(val id: Int, val en: Int, val qn: Int)
  extends BaseEngine[EvalInfo, Query, Prediction, Actual] {
    def train(
      sc: SparkContext, 
      engineParams: EngineParams,
      instanceId: String = "",
      params: WorkflowParams = WorkflowParams()
    ): Seq[Any] = {
      Seq[Any]()
    }

    def eval(
      sc: SparkContext, 
      engineParams: EngineParams, 
      params: WorkflowParams)
    : Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = {
      (0 until en).map { ex => {
        val qpas = (0 until qn).map { qx => {
          (Query(id, ex, qx), Prediction(id, ex, qx), Actual(id, ex, qx))
        }}
  
        (EvalInfo(id = id, ex = ex), sc.parallelize(qpas))
      }}
    }
  
  }

  /*
  class Evaluator0 extends Evaluator[EvalInfo, Query, Prediction, Actual,
      (Query, Prediction, Actual), 
      (EvalInfo, Seq[(Query, Prediction, Actual)]),
      Seq[(EvalInfo, (EvalInfo, Seq[(Query, Prediction, Actual)]))]
      ] {

    def evaluateUnit(q: Query, p: Prediction, a: Actual)
    : (Query, Prediction, Actual) = (q, p, a)

    def evaluateSet(
        evalInfo: EvalInfo, 
        eus: Seq[(Query, Prediction, Actual)])
    : (EvalInfo, Seq[(Query, Prediction, Actual)]) = (evalInfo, eus)

    def evaluateAll(
      input: Seq[(EvalInfo, (EvalInfo, Seq[(Query, Prediction, Actual)]))]) 
    = input
  }
  */

}

/*
class EvaluatorSuite
extends FunSuite with Inside with SharedSparkContext {
  import org.apache.predictionio.controller.TestEvaluator._
  @transient lazy val logger = Logger[this.type] 

  test("Evaluator.evaluate") {
    val engine = new FakeEngine(1, 3, 10)
    val evaluator = new Evaluator0()
  
    val evalDataSet = engine.eval(sc, null.asInstanceOf[EngineParams])
    val er: Seq[(EvalInfo, (EvalInfo, Seq[(Query, Prediction, Actual)]))] =
      evaluator.evaluateBase(sc, evalDataSet)

    evalDataSet.zip(er).map { case (input, output) => {
      val (inputEvalInfo, inputQpaRDD) = input
      val (outputEvalInfo, (outputEvalInfo2, outputQpaSeq)) = output
      
      inputEvalInfo shouldBe outputEvalInfo
      inputEvalInfo shouldBe outputEvalInfo2
      
      val inputQpaSeq: Array[(Query, Prediction, Actual)] = inputQpaRDD.collect

      inputQpaSeq.size should be (outputQpaSeq.size)
      // TODO. match inputQpa and outputQpa content.
    }}
  }
}
*/
