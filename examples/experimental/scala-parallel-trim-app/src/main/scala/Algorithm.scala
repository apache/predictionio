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

package org.apache.predictionio.examples.experimental.trimapp

import org.apache.predictionio.controller.P2LAlgorithm
import org.apache.predictionio.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

//case class AlgorithmParams(mult: Int) extends Params

//class Algorithm(val ap: AlgorithmParams)
class Algorithm
  extends P2LAlgorithm[TrainingData, Model, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(data: TrainingData): Model = {
    new Model
  }

  def predict(model: Model, query: Query): PredictedResult = {
    // Prefix the query with the model data
    PredictedResult(p = "")
  }
}

class Model extends Serializable {
  override def toString = "Model"
}
