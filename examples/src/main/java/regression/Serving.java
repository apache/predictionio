package io.prediction.engines.java.regression;

import io.prediction.controller.java.LJavaServing;
import io.prediction.controller.EmptyParams;

import java.lang.Iterable;

public class Serving extends LJavaServing<EmptyParams, Double[], Double> {
  public Serving(EmptyParams ep) {}

  public Double serve(Double[] query, Iterable<Double> predictions) {
    int n = 0;
    double s = 0.0;
    for (Double d: predictions) {
      n += 1;
      s += d;
    }
    return s / n;
  }
}
