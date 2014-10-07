package io.prediction.examples.java.parallel;

import io.prediction.controller.java.IJavaEngineFactory;
import io.prediction.controller.java.LJavaFirstServing;
import io.prediction.controller.java.PJavaEngine;
import io.prediction.controller.java.PJavaEngineBuilder;

import java.util.HashMap;

import org.apache.spark.api.java.JavaPairRDD;

public class EngineFactory implements IJavaEngineFactory {
  public PJavaEngine<JavaPairRDD<String, Float>, Object, JavaPairRDD<String, Float>, Query, Float,
        Object> apply() {
    return new PJavaEngineBuilder<JavaPairRDD<String, Float>, Object, JavaPairRDD<String, Float>,
          Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .preparatorClass(Preparator.class)
      .addAlgorithmClass("ParallelAlgorithm", Algorithm.class)
      .servingClass(Serving.class)
      .build();
  }
}
