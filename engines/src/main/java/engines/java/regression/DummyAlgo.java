package io.prediction.engines.java.regression;

import io.prediction.java.JavaLocalAlgorithm;
import io.prediction.EmptyParams;

public class DummyAlgo
  extends JavaLocalAlgorithm<TrainingData, Double[], Double, Double, EmptyParams> {
  public Double train(TrainingData data) {
    int n = data.r;
    double sumY = 0.0;
    for (int i=0; i<n; i++){
      sumY += data.y[i];
      System.out.println(data.y[i]);
    }
    return sumY / n;
  }

  public Double predict(Double model, Double[] query) {
    return model;
  }
}
