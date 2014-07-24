package io.prediction.engines.java.recommendations;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaSimpleEngine;
import io.prediction.controller.java.JavaSimpleEngineBuilder;

public class EvaluationEngineFactory implements IEngineFactory {
  public JavaSimpleEngine<TrainingData, EmptyParams, Query, Float, Float> apply() {
    return new JavaSimpleEngineBuilder<
      TrainingData, EmptyParams, Query, Float, Float> ()
      .dataSourceClass(EvaluationDataSource.class)
      .addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
      .servingClass()
      .build();
  }
}
