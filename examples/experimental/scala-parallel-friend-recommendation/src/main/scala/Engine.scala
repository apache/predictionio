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

package org.apache.predictionio.examples.pfriendrecommendation

import org.apache.predictionio.controller.IEngineFactory
import org.apache.predictionio.controller.Engine

case class Query(
  val item1: Long,
  val item2: Long
)

case class PredictedResult(
  val productScores: Array[ProductScore]
)

case class ProductScore(
  product: Int,
  score: Double
)

object PSimRankEngineFactory extends IEngineFactory {
  def apply() = {
    Engine(
      Map(
        "default" -> classOf[DataSource],
        "node" -> classOf[NodeSamplingDataSource],
        "forest" -> classOf[ForestFireSamplingDataSource]),
      classOf[IdentityPreparator],
      Map("simrank" -> classOf[SimRankAlgorithm]),
      classOf[Serving])
  }
}
