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

package org.apache.predictionio.examples.java.recommendations.tutorial4;

import org.apache.predictionio.controller.java.LJavaPreparator;
import org.apache.predictionio.controller.java.EmptyParams;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class Preparator extends LJavaPreparator<EmptyParams, TrainingData, PreparedData> {

  final static Logger logger = LoggerFactory.getLogger(Preparator.class);
  final int indexOffset = 5;

  public Preparator() {}

  public PreparedData prepare(TrainingData trainingData) {
    Map<Integer, RealVector> itemFeatures = new HashMap<Integer, RealVector>();

    int featureSize = trainingData.genres.size();

    for (Integer iid: trainingData.itemInfo.keySet()) {
      String[] info = trainingData.itemInfo.get(iid);

      RealVector features = new ArrayRealVector(featureSize);
      for (int i = 0; i < featureSize; i++) {
        features.setEntry(i, Double.parseDouble(info[i + indexOffset]));
      }
      itemFeatures.put(iid, features);
    }

    return new PreparedData(trainingData, itemFeatures, featureSize);
  }
}
