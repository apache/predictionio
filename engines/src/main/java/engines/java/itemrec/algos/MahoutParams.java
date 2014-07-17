package io.prediction.engines.java.itemrec.algos;

import io.prediction.api.Params;

public class MahoutParams implements Params {
  public int numRecommendations;
  public MahoutParams(int numRecommendations) {
    this.numRecommendations = numRecommendations;
  }
}
