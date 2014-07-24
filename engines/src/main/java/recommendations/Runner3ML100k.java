package io.prediction.engines.java.recommendations;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.workflow.JavaAPIDebugWorkflow;

import java.util.HashMap;

import io.prediction.controller.IdentityPreparator;

public class Runner3ML100k {

  public static void runEvaluation() {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams(
        "engines/src/main/java/recommendations/testdata/u.data"))
      .addAlgorithmParams("MyRecommendationAlgo", new AlgoParams(0.1))
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "MyEngine",
      new HashMap<String, String>(),
      3, // verbose
      (new EvaluationEngineFactory()).apply(),
      engineParams,
      Metrics.class,
      new EmptyParams()
    );
  }

  public static void main(String[] args) {
    runEvaluation();
  }
}
