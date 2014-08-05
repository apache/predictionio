package io.prediction.engines.java.recommendations.tutorial4;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaEngine;
import io.prediction.controller.java.JavaEngineBuilder;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.workflow.JavaAPIDebugWorkflow;

import java.util.HashMap;

import io.prediction.controller.IdentityPreparator;

public class Runner4b {
  
  private static class HalfBakedEngineFactory implements IEngineFactory {
    public JavaEngine<TrainingData, EmptyParams, PreparedData, Query, Float, Object> apply() {
      return new JavaEngineBuilder<
        TrainingData, EmptyParams, PreparedData, Query, Float, Object> ()
        .dataSourceClass(DataSource.class)
        .preparatorClass(Preparator.class)
        .build();
    }
  }

  public static void main(String[] args) {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams("data/ml-100k/", true))
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "Recommendation.tutorial4.Runner4b", 
      new HashMap<String, String>(),
      3, // verbose
      (new HalfBakedEngineFactory()).apply(),
      engineParams
    );
  }
}

