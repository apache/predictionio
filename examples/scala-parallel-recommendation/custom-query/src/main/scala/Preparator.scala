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

package org.template.recommendation

import org.apache.predictionio.controller.PPreparator

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class Preparator extends PPreparator[TrainingData, PreparedData] {
  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData =
    new PreparedData(ratings = trainingData.ratings, items = trainingData.items)
}

// HOWTO: added items(movies) list to prepared data to have possiblity to sort
// them in predict stage.
class PreparedData(val ratings: RDD[Rating], val items: RDD[(String, Item)])
  extends Serializable
