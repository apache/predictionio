package io.prediction.examples.java.recommendations.tutorial4;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaEngine;
import io.prediction.controller.java.JavaEngineBuilder;

public class EngineFactory implements IEngineFactory {
  public JavaEngine<TrainingData, EmptyParams, PreparedData, Query, Float, Object> apply() {
    return new JavaEngineBuilder<
      TrainingData, EmptyParams, PreparedData, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .preparatorClass(Preparator.class)
      .addAlgorithmClass("featurebased", FeatureBasedAlgorithm.class)
      .addAlgorithmClass("collaborative", CollaborativeFilteringAlgorithm.class)
      .servingClass(Serving.class)
      .build();
  }
}
