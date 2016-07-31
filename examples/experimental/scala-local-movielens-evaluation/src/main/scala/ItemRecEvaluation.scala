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

package org.apache.predictionio.examples.mlc

import org.apache.predictionio.engines.itemrec.ItemRecEngine
import org.apache.predictionio.engines.itemrec.EventsDataSourceParams
import org.apache.predictionio.engines.itemrec.PreparatorParams
import org.apache.predictionio.engines.itemrec.NCItemBasedAlgorithmParams
import org.apache.predictionio.engines.itemrec.EvalParams
import org.apache.predictionio.engines.itemrec.ItemRecEvaluator
import org.apache.predictionio.engines.itemrec.ItemRecEvaluatorParams
import org.apache.predictionio.engines.itemrec.MeasureType
import org.apache.predictionio.engines.base.EventsSlidingEvalParams
import org.apache.predictionio.engines.base.BinaryRatingParams

import org.apache.predictionio.controller.EngineParams
import org.apache.predictionio.controller.Workflow
import org.apache.predictionio.controller.WorkflowParams

import com.github.nscala_time.time.Imports._

// Recommend to run with "--driver-memory 2G"
object ItemRecEvaluation1 {
  def main(args: Array[String]) {
    val engine = ItemRecEngine()
    
    val dsp = EventsDataSourceParams(
      appId = 9,
      actions = Set("rate"),
      attributeNames = CommonParams.DataSourceAttributeNames,
      slidingEval = Some(new EventsSlidingEvalParams(
        firstTrainingUntilTime = new DateTime(1998, 2, 1, 0, 0),
        evalDuration = Duration.standardDays(7),
        evalCount = 12)),
        //evalCount = 3)),
      evalParams = Some(new EvalParams(queryN = 10))
    )
  
    val pp = new PreparatorParams(
      actions = Map("rate" -> None),
      seenActions = Set("rate"),
      conflict = "latest")

    val ncMahoutAlgoParams = new NCItemBasedAlgorithmParams(
      booleanData = true,
      itemSimilarity = "LogLikelihoodSimilarity",
      weighted = false,
      threshold = 4.9E-324,
      nearestN = 10,
      unseenOnly = false,
      freshness = 0,
      freshnessTimeUnit = 86400)
    
    val engineParams = new EngineParams(
      dataSourceParams = dsp,
      preparatorParams = pp,
      algorithmParamsList = Seq(
        ("ncMahoutItemBased", ncMahoutAlgoParams)))

    val evaluatorParams = new ItemRecEvaluatorParams(
      ratingParams = new BinaryRatingParams(
        actionsMap = Map("rate" -> None),
        goodThreshold = 3),
      measureType = MeasureType.PrecisionAtK,
      measureK = 10
    ) 
  
    Workflow.runEngine(
      params = WorkflowParams(batch = "MLC: ItemRec Evaluation1", verbose = 0),
      engine = engine,
      engineParams = engineParams,
      evaluatorClassOpt = Some(classOf[ItemRecEvaluator]),
      evaluatorParams = evaluatorParams
    )
  }
}
