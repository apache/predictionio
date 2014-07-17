package io.prediction.engines.java.regression;

import io.prediction.controller.java.LJavaAlgorithm;

public class DefaultAlgorithm
  extends LJavaAlgorithm<DefaultAlgorithmParams, TrainingData, Object, Double[], Double> {
  public final DefaultAlgorithmParams p; 

  public DefaultAlgorithm(DefaultAlgorithmParams p) {
    this.p = p;
  }

  public Object train(TrainingData data) {
    return null;
  }

  public Double predict(Object nullModel, Double[] query) {
    return p.v;
  }
}

