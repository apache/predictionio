package io.prediction.engines.java.regression;

import io.prediction.java.JavaServer;
import io.prediction.EmptyParams;
import java.lang.Iterable;

// Average over all predictions;
public class Server
  extends JavaServer<Double[], Double, EmptyParams> {
  public Double combine(Double[] feature, Iterable<Double> predictions) {
    int n = 0;
    double s = 0.0;
    for (Double d: predictions) {
      n += 1;
      s += d;
    }
    return s / n;
  }
}
