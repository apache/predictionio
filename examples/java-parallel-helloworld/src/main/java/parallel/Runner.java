package io.prediction.examples.java.parallel;

import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.EmptyParams;
import io.prediction.controller.java.IJavaEngineFactory;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.controller.java.JavaWorkflow;
import io.prediction.controller.java.PJavaEngine;
import io.prediction.controller.java.PJavaEngineBuilder;
import io.prediction.controller.java.WorkflowParamsBuilder;

import java.util.HashMap;

import org.apache.spark.api.java.JavaPairRDD;

public class Runner {

  // During development, one can build a semi-engine, only add the first few layers. In this
  // particular example, we only add until dataSource layer
  private static class HalfBakedEngineFactory implements IJavaEngineFactory {
    public PJavaEngine<JavaPairRDD<String, Float>, Object, JavaPairRDD<String, Float>,
        Query, Float, Object> apply() {
      return new PJavaEngineBuilder<
        JavaPairRDD<String, Float>, Object, JavaPairRDD<String, Float>, Query, Float, Object> ()
        .dataSourceClass(DataSource.class)
        .preparatorClass(Preparator.class)
        .addAlgorithmClass("ParallelAlgorithm", Algorithm.class)
        .servingClass(Serving.class)
        .build();
    }
  }

  public static void runComponents() {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .addAlgorithmParams("ParallelAlgorithm", new EmptyParams())
      .servingParams(new EmptyParams())
      .build();
    JavaWorkflow.runEngine(
      (new HalfBakedEngineFactory()).apply(),
      engineParams,
      null,
      new EmptyParams(),
      new WorkflowParamsBuilder().batch("MyEngine").verbose(3).build()
    );
  }

  public static void main(String[] args) {
    runComponents();
  }
}
