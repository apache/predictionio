package io.prediction.engines.java.recommendations.tutorial1;

import io.prediction.controller.Params;

public class AlgoParams implements Params {
  public double threshold;

  public AlgoParams(double threshold) {
    this.threshold = threshold;
  }
}
