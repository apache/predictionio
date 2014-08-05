package io.prediction.engines.java.regression;

import io.prediction.controller.Params;

public class DefaultAlgorithmParams implements Params {
  public final double v;
  public DefaultAlgorithmParams(double v) {
    this.v = v;
  }

  @Override
  public String toString() {
    return "DefaultAlgorithmParams (k=" + this.v + ")";
  }
}


