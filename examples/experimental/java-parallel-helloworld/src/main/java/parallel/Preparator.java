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

package org.apache.predictionio.examples.java.parallel;

import org.apache.predictionio.controller.java.EmptyParams;
import org.apache.predictionio.controller.java.PJavaPreparator;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

public class Preparator extends
  PJavaPreparator<EmptyParams, JavaPairRDD<String, Float>, JavaPairRDD<String, Float>> {

  @Override
  public JavaPairRDD<String, Float> prepare(JavaSparkContext jsc,
      JavaPairRDD<String, Float> data) {
    return data.mapValues(new Function<Float, Float>() {
        @Override
        public Float call(Float temperature) {
          // let's convert it to degrees Celsius
          return (temperature - 32.0f) / 9 * 5;
        }
      });
  }
}
