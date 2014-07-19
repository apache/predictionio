package io.prediction.engines.java.regression;

import io.prediction.controller.Params;

public class PreparatorParams implements Params {
  // Take the r-fraction of data in training.
  public double r = 1.0;
  public PreparatorParams(double r) {
    this.r = r;
  }
}


