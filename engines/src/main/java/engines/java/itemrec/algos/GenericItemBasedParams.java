package io.prediction.engines.java.itemrec.algos;

import io.prediction.BaseParams;

public class GenericItemBasedParams implements BaseParams {
  // TODO: add more algo specific params
  public int numRecommendations;
  public GenericItemBasedParams(int numRecommendations) {
    this.numRecommendations = numRecommendations;
  }
}
