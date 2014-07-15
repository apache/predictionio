package io.prediction.engines.java.itemrec.algos;

import io.prediction.BaseParams;

public class SVDPlusPlusParams implements BaseParams {
  // TODO: add more algo specific params
  public int numRecommendations;
  public SVDPlusPlusParams(int numRecommendations) {
    this.numRecommendations = numRecommendations;
  }
}
