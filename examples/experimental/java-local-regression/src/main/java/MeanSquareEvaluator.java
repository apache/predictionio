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

package org.apache.predictionio.examples.java.regression;

import org.apache.predictionio.controller.java.JavaEvaluator;
import java.lang.Iterable;
import org.apache.predictionio.controller.java.EmptyParams;
import scala.Tuple2;
import java.util.List;
import java.util.ArrayList;


public class MeanSquareEvaluator
  extends JavaEvaluator<EmptyParams, Integer,
          Double[], Double, Double, Double, Double, String> {

  public Double evaluateUnit(Double[] query, Double prediction, Double actual) {
    return (prediction - actual) * (prediction - actual);
  }

  public Double evaluateSet(Integer dp, Iterable<Double> mseSeq) {
    double mse = 0.0;
    int n = 0;
    for (Double e: mseSeq) {
      mse += e;
      n += 1;
    }
    return mse / n;
  }

  public String evaluateAll(Iterable<Tuple2<Integer, Double>> input) {
    List<String> l = new ArrayList<String>();
    for (Tuple2<Integer, Double> t : input) {
      l.add("MSE: " + t._2().toString());
    }
    return l.toString();
  }
}
