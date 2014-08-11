package io.prediction.examples.java.recommendations.tutorial5;

import io.prediction.examples.java.recommendations.tutorial1.DataSourceParams;
import io.prediction.examples.java.recommendations.tutorial3.Metrics;

import io.prediction.controller.java.EmptyParams;
import io.prediction.controller.java.IJavaEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.workflow.JavaAPIDebugWorkflow;

import java.util.HashMap;

public class Runner5 {

  public static void runEvaluation(String filePath) {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams(filePath))
      .addAlgorithmParams("MyMahoutRecommendationAlgo",
        new MahoutAlgoParams("LogLikelihoodSimilarity"))
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "MyEngine",
      new HashMap<String, String>(),
      3, // verbose
      (new EngineFactory()).apply(),
      engineParams,
      Metrics.class,
      new EmptyParams()
    );
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Error: Please specify the file path as argument");
      System.exit(1);
    }
    runEvaluation(args[0]);
    System.exit(0); // clean shutdown is needed for spark
  }
}
