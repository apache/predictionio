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

import java.io.Serializable;
import java.util.Arrays;

public class TrainingData implements Serializable {
  public final Double[][] x;
  public final Double[] y;
  public final int r;
  public final int c;
  public TrainingData(Double[][] x, Double[] y) {
    this.x = x;
    this.y = y;
    this.r = x.length;
    this.c = x[0].length;
  }
  @Override public String toString() {
    return "TrainingData: r=" + this.r + ",c=" + this.c;
  }
}
