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

package pio.refactor

import org.apache.predictionio.controller.IEngineFactory
import org.apache.predictionio.controller.Engine
import org.apache.predictionio.controller._
//import org.apache.predictionio.workflow.CoreWorkflow
import grizzled.slf4j.Logger

case class Query(q: Int)

case class PredictedResult(p: Int)

case class ActualResult()

object VanillaEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      //classOf[Preparator],
      PIdentityPreparator(classOf[DataSource]),
      Map("algo" -> classOf[Algorithm]),
      classOf[Serving])
  }
}

object Runner {
  @transient lazy val logger = Logger[this.type]

  def main(args: Array[String]) {
    val engine = VanillaEngine()
    val engineParams = EngineParams(
      algorithmParamsList = Seq(("algo", AlgorithmParams(2)))
    )

    logger.error("Runner. before evaluation!!!")
    val evaluator = new VanillaEvaluator() 
    
    logger.error("Runner before runEval!!!")
    Workflow.runEval(
      engine = engine,
      engineParams = engineParams,
      evaluator = evaluator,
      evaluatorParams = EmptyParams())

  }
}
