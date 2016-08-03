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
import org.apache.predictionio.controller.java.JavaEngineParams;
import org.apache.predictionio.controller.java.JavaEngineParamsBuilder;
import org.apache.predictionio.controller.java.LJavaFirstServing;
import org.apache.predictionio.controller.java.JavaWorkflow;
import org.apache.predictionio.controller.java.WorkflowParamsBuilder;

import java.util.HashMap;

import org.apache.predictionio.controller.IdentityPreparator;

public class Runner4d {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Error: Please specify the file directory as argument");
      System.exit(1);
    }

    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams(args[0], true))
      // 1 -> -1., 2 -> -.5, 3 -> 0., 4 -> .5, 5 -> 1.
      .addAlgorithmParams("featurebased", new FeatureBasedAlgorithmParams(1.0, 5.0, 3.0, 0.5))
      .addAlgorithmParams("featurebased", new FeatureBasedAlgorithmParams(4.0, 5.0, 3.0, 0.5))
      .build();

    JavaWorkflow.runEngine(
      (new EngineFactory()).apply(),
      engineParams,
      new WorkflowParamsBuilder().batch("Recommendation.tutorial4.Runner4d").verbose(3).build()
    );
  }
}



