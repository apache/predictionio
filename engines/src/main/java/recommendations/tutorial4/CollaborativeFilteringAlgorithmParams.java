package io.prediction.engines.java.recommendations.tutorial4;

import io.prediction.controller.Params;

public class CollaborativeFilteringAlgorithmParams implements Params {
  public double threshold;

  public CollaborativeFilteringAlgorithmParams(double threshold) {
    this.threshold = threshold;
  }
}
