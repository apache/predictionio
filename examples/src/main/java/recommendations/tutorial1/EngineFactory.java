package io.prediction.examples.java.recommendations.tutorial1;

import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;

public class EngineFactory implements IEngineFactory {
  public JavaSimpleEngine<TrainingData, Object, Query, Float, Object> apply() {
    return new JavaSimpleEngineBuilder<
      TrainingData, Object, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .preparatorClass() // Use default Preparator
      .addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
      .servingClass() // Use default Serving
      .build();
  }
}
