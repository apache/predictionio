package io.prediction.engines.java.regression;

import io.prediction.api.IEngineFactory;
import io.prediction.api.java.JavaEngine;
import io.prediction.api.java.JavaEngineBuilder;

public class EngineFactory implements IEngineFactory {
  public JavaEngine<TrainingData, Integer, TrainingData, Double[], Double, Double> apply() {
    return new JavaEngineBuilder<TrainingData, Integer, TrainingData, Double[], Double, Double> ()
      .dataSourceClass(DataSource.class)
      .preparatorClass(Preparator.class)
      .addAlgorithmClass("OLS", OLSAlgorithm.class)
      .addAlgorithmClass("Default", DefaultAlgorithm.class)
      .servingClass(Serving.class)
      .build();
  }
}
