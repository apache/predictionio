package io.prediction.examples.java.regression;

import io.prediction.controller.java.EmptyParams;
import io.prediction.controller.java.IJavaEngineFactory;
import io.prediction.controller.java.JavaParams;
import io.prediction.controller.java.JavaEngine;
import io.prediction.controller.java.JavaEngineBuilder;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.controller.java.LJavaAlgorithm;
import io.prediction.workflow.APIDebugWorkflow;
import io.prediction.workflow.JavaAPIDebugWorkflow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import scala.Tuple2;

public class Run {
  // During development, one can build a semi-engine, only add the first few layers. In this
  // particular example, we only add until Algorithm Layer.
  private static class HalfBakedEngineFactory implements IJavaEngineFactory {
    public JavaEngine<TrainingData, Integer, TrainingData, Double[], Double, Double> apply() {
      return new JavaEngineBuilder<TrainingData, Integer, TrainingData, Double[], Double, Double> ()
        .dataSourceClass(DataSource.class)
        .preparatorClass(Preparator.class)
        .addAlgorithmClass("OLS", OLSAlgorithm.class)
        .addAlgorithmClass("Default", DefaultAlgorithm.class)
        .build();
    }
  }

  // Deprecated. Use Engine Builder
  public static void runComponents () {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams("data/lr_data.txt"))
      .preparatorParams(new PreparatorParams(0.3))
      .addAlgorithmParams("OLS", new EmptyParams())
      .addAlgorithmParams("Default", new DefaultAlgorithmParams(0.2))
      .addAlgorithmParams("Default", new DefaultAlgorithmParams(0.4))
      .build();

    JavaAPIDebugWorkflow.runEngine(
        "java regression engine",
        new HashMap<String, String>(),
        3,  // verbose
        (new HalfBakedEngineFactory()).apply(),
        engineParams
        );
  }

  public static void runEngine() {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams("data/lr_data.txt"))
      .preparatorParams(new PreparatorParams(0.3))
      .addAlgorithmParams("OLS", new EmptyParams())
      .addAlgorithmParams("Default", new DefaultAlgorithmParams(0.2))
      .addAlgorithmParams("Default", new DefaultAlgorithmParams(0.4))
      .build();

    JavaAPIDebugWorkflow.runEngine(
        "java regression engine",
        new HashMap<String, String>(),
        3,  // verbose
        (new EngineFactory()).apply(),
        engineParams,
        MeanSquareMetrics.class,
        new EmptyParams()
        );
  }

  public static void main(String[] args) {
    //runEngine();
    runComponents();
  }
}
