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

import org.apache.predictionio.controller.java.EmptyParams;
import org.apache.predictionio.controller.java.IJavaEngineFactory;
import org.apache.predictionio.controller.java.JavaEngine;
import org.apache.predictionio.controller.java.JavaEngineBuilder;

public class EngineFactory implements IJavaEngineFactory {
  public JavaEngine<TrainingData, EmptyParams, PreparedData, Query, Float, Object> apply() {
    return new JavaEngineBuilder<
      TrainingData, EmptyParams, PreparedData, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .preparatorClass(Preparator.class)
      .addAlgorithmClass("featurebased", FeatureBasedAlgorithm.class)
      .addAlgorithmClass("collaborative", CollaborativeFilteringAlgorithm.class)
      .servingClass(Serving.class)
      .build();
  }
}
