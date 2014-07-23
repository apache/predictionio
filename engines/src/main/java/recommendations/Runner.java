package myrecommendations;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.workflow.JavaAPIDebugWorkflow;

import java.util.HashMap;

import io.prediction.controller.IdentityPreparator;

public class Runner {

  // During development, one can build a semi-engine, only add the first few layers. In this
  // particular example, we only add until dataSource layer
  private static class HalfBakedEngineFactory implements IEngineFactory {
    public JavaSimpleEngine<TrainingData, EmptyParams, Query, Float, EmptyData> apply() {
      return new JavaSimpleEngineBuilder<
        TrainingData, EmptyParams, Query, Float, EmptyData> ()
        .dataSourceClass(DataSource.class)
        //.addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
        .build();
    }
  }

  public static void runComponents() {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams(
        "engines/src/main/java/recommendations/testdata/ratings.csv"))
      //.addAlgorithmParams("MyRecommendationAlgo", new EmptyParams())
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "MyEngine",
      new HashMap<String, String>(),
      3, // verbose
      (new HalfBakedEngineFactory()).apply(),
      engineParams,
      null,
      new EmptyParams()
    );
  }

  public static void main(String[] args) {
    //runEngine();
    runComponents();
  }
}
