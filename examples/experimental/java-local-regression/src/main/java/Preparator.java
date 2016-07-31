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

import org.apache.predictionio.controller.java.LJavaPreparator;

// This Preparator is just a proof-of-concept. It removes a fraction of the
// training data to make training more "efficient".
public class Preparator extends LJavaPreparator<PreparatorParams, TrainingData, TrainingData> {
  private PreparatorParams pp;
  public Preparator(PreparatorParams pp) {
    this.pp = pp;
  }

  public TrainingData prepare(TrainingData td) {
    int n = (int) (td.r * pp.r);

    Double[][] x = new Double[n][td.c];
    Double[] y = new Double[n];
    for (int i=0; i<n; i++) {
      for (int j=0; j<td.c; j++) {
        x[i][j] = td.x[i][j];
      }
      y[i] = td.y[i];
    }
    return new TrainingData(x, y);
  }
}
