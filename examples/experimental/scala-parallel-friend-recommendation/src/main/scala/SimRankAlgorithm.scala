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
import org.apache.predictionio.controller.PAlgorithm
import org.apache.predictionio.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

case class SimRankParams(
  val numIterations: Int,
  val decay: Double) extends Params

class SimRankAlgorithm(val ap: SimRankParams)
  extends PAlgorithm[TrainingData, RDD[(Long,Double)], Query, Double] {

  def train(td: TrainingData): RDD[(Long,Double)] = {
    td.g.edges.count()
    val scores = DeltaSimRankRDD.compute(
      td.g,
      ap.numIterations,
      td.identityMatrix,
      ap.decay)
    scores
  }

  /*
  def batchPredict(
    model: RDD[(VertexId,Double)],
    feature: RDD[(Long, (Int, Int))]): RDD[(Long, Double)] = {
    feature.map(x => (x._1, predict(model, (x._2._1, x._2._1))))
  }
  */

  def predict(
    model: RDD[(Long,Double)], query:Query): Double = {
    // Will never encounter rounding errors because model is an n*n "matrix".
    val numElems = math.sqrt(model.count()).toInt
    val index = query.item1 * numElems + query.item2
    val seq = model.lookup(index)
    seq.head
 }
}
