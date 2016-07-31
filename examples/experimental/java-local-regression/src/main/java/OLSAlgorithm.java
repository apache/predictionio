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

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.linear.RealVector;
import java.util.Arrays;
import org.apache.predictionio.controller.java.EmptyParams;

import org.apache.predictionio.controller.java.LJavaAlgorithm;

public class OLSAlgorithm
  extends LJavaAlgorithm<EmptyParams, TrainingData, Double[], Double[], Double> {

  public Double[] train(TrainingData data) {
    OLSMultipleLinearRegression r = new OLSMultipleLinearRegression();
    // Convert Double[][] to double[][]
    double[][] x = new double[data.r][data.c];
    double[] y = new double[data.r];

    for (int i=0; i<data.r; i++) {
      for (int j=0; j<data.c; j++) {
        x[i][j] = data.x[i][j].doubleValue();
      }
      y[i] = data.y[i].doubleValue();
    }

    r.newSampleData(y, x);
    // Fixme. Add intercept
    double[] p = r.estimateRegressionParameters();
    Double[] pp = new Double[data.c];
    for (int j=0; j<data.c; j++) {
      pp[j] = p[j];
    }
    System.out.println("Regression Algo: " + Arrays.toString(pp));
    return pp;
  }

  public Double predict(Double[] model, Double[] query) {
    double dotProduct = 0.0;
    for (int i = 0; i < model.length; i++) {
      dotProduct += model[i] * query[i];
    }
    return dotProduct;
  }
}
