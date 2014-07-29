/*
package io.prediction.engines.java.recommendations;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;

public class EngineFactory implements IEngineFactory {
  public JavaSimpleEngine<TrainingData, EmptyParams, Query, Float, Object> apply() {
    return new JavaSimpleEngineBuilder<
      TrainingData, EmptyParams, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
      .servingClass()
      .build();
  }
}
*/
