package io.prediction.engines.java.recommendations.tutorial1;

import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;

public class EngineFactory implements IEngineFactory {
  public JavaSimpleEngine<TrainingData, Object, Query, Float, Object> apply() {
    return new JavaSimpleEngineBuilder<
      TrainingData, Object, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
      .servingClass()
      .build();
  }
}
