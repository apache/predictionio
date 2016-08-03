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

package org.apache.predictionio.examples.friendrecommendation

import org.apache.predictionio.controller._
import scala.collection.immutable.HashMap
import scala.math
import scala.io.Source

class KeywordSimilarityAlgorithm (val ap: FriendRecommendationAlgoParams)
  extends LAlgorithm[FriendRecommendationTrainingData,
    KeywordSimilarityModel, FriendRecommendationQuery, 
    FriendRecommendationPrediction] {

  override
  def train(td: FriendRecommendationTrainingData): KeywordSimilarityModel = {
    var keywordSimWeight = 1.0
    var keywordSimThreshold = 1.0
    // Originally for the purpose of training an acceptance threshold
    // Commented out here due to the high time and space complexity of training
    /*
    td.trainingRecord.foreach{
      record =>
      val sim = findKeywordSimilarity(td.userKeyword(record._1),
                                      td.itemKeyword(record._2))
      val prediction = (keywordSimWeight * sim - keywordSimThreshold >= 0)
      if (prediction != record._3) {
        val y = if (record._3) 1 else -1
        keywordSimWeight += y * sim
        keywordSimThreshold += y * -1
      }
    }
    */
    new KeywordSimilarityModel(td.userIdMap, 
      td.itemIdMap, td.userKeyword, td.itemKeyword, 
      keywordSimWeight, keywordSimThreshold)
  }

  def findKeywordSimilarity(keywordMap1: HashMap[Int, Double], 
    keywordMap2: HashMap[Int, Double]): 
    Double = {
    var similarity = 0.0
    keywordMap1.foreach(kw => 
      similarity += kw._2 * keywordMap2.getOrElse(kw._1, 0.0))
    similarity
  }

  override
  def predict(model: KeywordSimilarityModel, 
    query: FriendRecommendationQuery): 
    FriendRecommendationPrediction = {
    // Currently use empty map for unseen users or items
    if (model.userIdMap.contains(query.user) && 
      model.itemIdMap.contains(query.item)) {
      val confidence = findKeywordSimilarity(
        model.userKeyword(model.userIdMap(query.user)),
        model.itemKeyword(model.itemIdMap(query.item)))
      val acceptance = ((confidence * model.keywordSimWeight)
        >= model.keywordSimThreshold)
      new FriendRecommendationPrediction(confidence, acceptance)
    } else {
      val confidence = 0
      val acceptance = ((confidence * model.keywordSimWeight)
        >= model.keywordSimThreshold)
      new FriendRecommendationPrediction(confidence, acceptance)
    }
  }
}
