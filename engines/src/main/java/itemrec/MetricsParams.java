package io.prediction.engines.java.itemrec;

import io.prediction.controller.Params;

public class MetricsParams implements Params {
  public int k;

  public MetricsParams(int k) {
    this.k = k;
  }
}
