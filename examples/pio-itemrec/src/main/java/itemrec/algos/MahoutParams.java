package io.prediction.engines.java.itemrec.algos;

import io.prediction.controller.java.JavaParams;

public class MahoutParams implements JavaParams {
  public int numRecommendations;
  public MahoutParams(int numRecommendations) {
    this.numRecommendations = numRecommendations;
  }
}
