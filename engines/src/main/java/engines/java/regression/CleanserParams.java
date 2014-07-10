package io.prediction.engines.java.regression;

import io.prediction.BaseParams;

public class CleanserParams implements BaseParams {
  // Take the r-fraction of data in training.
  public double r = 1.0;
  public CleanserParams(double r) {
    this.r = r;
  }
}

