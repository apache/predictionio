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

package org.apache.predictionio.examples.stock

import org.apache.predictionio.controller.LAlgorithm

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import org.apache.predictionio.controller.EmptyParams
import org.saddle._

import scala.reflect._
import scala.reflect.runtime.universe._

import scala.collection.immutable.HashMap

abstract class StockStrategy[M: ClassTag]
  extends LAlgorithm[
      TrainingData, 
      (TrainingData, M), 
      QueryDate, 
      Prediction] {
  def train(trainingData: TrainingData): (TrainingData, M) = {
    (trainingData, createModel(trainingData.view))
  }

  def createModel(dataView: DataView): M

  def predict(dataModel: (TrainingData, M), queryDate: QueryDate)
  : Prediction = {
    val (trainingData, model) = dataModel

    val rawData = trainingData.rawDataB.value

    val dataView: DataView = 
      rawData.view(queryDate.idx, trainingData.maxWindowSize)

    val active = rawData._activeFrame

    val activeTickers = dataView
      .activeFrame()
      .rowAt(0)
      .filter(identity)
      .index.toVec.contents


    val query = Query(
      idx = queryDate.idx, 
      dataView = dataView,
      tickers = activeTickers,
      mktTicker = rawData.mktTicker)

    val prediction: Prediction = onClose(model, query)

    return prediction
  }

  def onClose(model: M, query: Query): Prediction
}

class EmptyStrategy extends StockStrategy[AnyRef] {
  def createModel(dataView: DataView): AnyRef = None

  def onClose(model: AnyRef, query: Query): Prediction = 
    Prediction(HashMap[String, Double]())
}
