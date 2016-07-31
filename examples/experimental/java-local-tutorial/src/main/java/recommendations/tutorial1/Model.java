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

package org.apache.predictionio.examples.java.recommendations.tutorial1;

import java.io.Serializable;
import java.util.Map;
import org.apache.commons.math3.linear.RealVector;

public class Model implements Serializable {
  public Map<Integer, RealVector> itemSimilarity;
  public Map<Integer, RealVector> userHistory;

  public Model(Map<Integer, RealVector> itemSimilarity,
    Map<Integer, RealVector> userHistory) {
    this.itemSimilarity = itemSimilarity;
    this.userHistory = userHistory;
  }

  @Override
  public String toString() {
    String s;
    if ((itemSimilarity.size() > 20) || (userHistory.size() > 20)) {
      s = "Model: [itemSimilarity.size=" + itemSimilarity.size() + "]\n"
        +"[userHistory.size=" + userHistory.size() + "]";
    } else {
      s = "Model: [itemSimilarity: " + itemSimilarity.toString() + "]\n"
      +"[userHistory: " + userHistory.toString() + "]";
    }
    return s;
  }
}
