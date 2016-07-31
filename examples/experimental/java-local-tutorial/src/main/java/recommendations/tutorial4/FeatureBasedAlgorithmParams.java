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

import org.apache.predictionio.controller.java.JavaParams;

// actual score = (rating - drift) / scale if min <= rating <= max
// if rating is outside [min, max], that scoring will not be used.
public class FeatureBasedAlgorithmParams implements JavaParams {
  public final double min;
  public final double max;
  public final double drift;
  public final double scale;

  public FeatureBasedAlgorithmParams(double min, double max, double drift, double scale) {
    this.min = min;
    this.max = max;
    this.drift = drift;
    this.scale = scale;
  }
  
  public FeatureBasedAlgorithmParams(double min, double max) {
    this(min, max, 0.0, 1.0);
  }
}

