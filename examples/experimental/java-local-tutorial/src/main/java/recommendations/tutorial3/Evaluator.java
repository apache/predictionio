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

package org.apache.predictionio.examples.java.recommendations.tutorial3;

import org.apache.predictionio.examples.java.recommendations.tutorial1.Query;
import org.apache.predictionio.controller.java.JavaEvaluator;
import org.apache.predictionio.controller.java.EmptyParams;

import scala.Tuple2;
import java.util.Arrays;
import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Root mean square error */
public class Evaluator
  extends JavaEvaluator<EmptyParams, Object, Query, Float, Float,
  Double, Double, String> {

  final static Logger logger = LoggerFactory.getLogger(Evaluator.class);

  @Override
  public Double evaluateUnit(Query query, Float predicted, Float actual) {
    logger.info("Q: " + query.toString() + " P: " + predicted + " A: " + actual);
    // return squared error
    double error;
    if (predicted.isNaN())
      error = -actual;
    else
      error = predicted - actual;
    return (error * error);
  }

  @Override
  public Double evaluateSet(Object dataParams, Iterable<Double> metricUnits) {
    double sum = 0.0;
    int count = 0;
    for (double squareError : metricUnits) {
      sum += squareError;
      count += 1;
    }
    return Math.sqrt(sum / count);
  }

  @Override
  public String evaluateAll(
    Iterable<Tuple2<Object, Double>> input) {
    return Arrays.toString(IteratorUtils.toArray(input.iterator()));
  }
}
