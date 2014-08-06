package io.prediction.examples.java.itemrec.algos;

import io.prediction.controller.Params;

public class MahoutParams implements Params {
  public int numRecommendations;
  public MahoutParams(int numRecommendations) {
    this.numRecommendations = numRecommendations;
  }
}
