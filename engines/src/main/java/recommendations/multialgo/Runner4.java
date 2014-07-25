package io.prediction.engines.java.recommendations.multialgo;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;
import io.prediction.controller.java.JavaEngine;
import io.prediction.controller.java.JavaEngineBuilder;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.workflow.JavaAPIDebugWorkflow;

import java.util.HashMap;

import io.prediction.controller.IdentityPreparator;

public class Runner4 {
  
  private static class HalfBakedEngineFactory implements IEngineFactory {
    public JavaSimpleEngine<TrainingData, EmptyParams, Query, Float, Object> apply() {
      return new JavaSimpleEngineBuilder<
        TrainingData, EmptyParams, Query, Float, Object> ()
        .dataSourceClass(DataSource.class)
        .build();
    }
  }

  public static void main(String[] args) {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams("data/ml-100k/"))
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "Recommendation.multialgo",
      new HashMap<String, String>(),
      3, // verbose
      (new HalfBakedEngineFactory()).apply(),
      engineParams
    );
  }
}
