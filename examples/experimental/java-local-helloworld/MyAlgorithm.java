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

package org.sample.java.helloworld;

import org.apache.predictionio.controller.java.*;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgorithm extends LJavaAlgorithm<
  EmptyAlgorithmParams, MyTrainingData, MyModel, MyQuery, MyPredictedResult> {

  final static Logger logger = LoggerFactory.getLogger(MyAlgorithm.class);

  @Override
  public MyModel train(MyTrainingData data) {
    Map<String, Double> sumMap = new HashMap<String, Double>();
    Map<String, Integer> countMap = new HashMap<String, Integer>();

    // calculate sum and count for each day
    for (MyTrainingData.DayTemperature temp : data.temperatures) {
      Double sum = sumMap.get(temp.day);
      Integer count = countMap.get(temp.day);
      if (sum == null) {
        sumMap.put(temp.day, temp.temperature);
        countMap.put(temp.day, 1);
      } else {
        sumMap.put(temp.day, sum + temp.temperature);
        countMap.put(temp.day, count + 1);
      }
    }

    // calculate the average
    Map<String, Double> averageMap = new HashMap<String, Double>();
    for (Map.Entry<String, Double> entry : sumMap.entrySet()) {
      String day = entry.getKey();
      Double average = entry.getValue() / countMap.get(day);
      averageMap.put(day, average);
    }

    return new MyModel(averageMap);
  }

  @Override
  public MyPredictedResult predict(MyModel model, MyQuery query) {
    Double temp = model.temperatures.get(query.day);
    return  new MyPredictedResult(temp);
  }
}
